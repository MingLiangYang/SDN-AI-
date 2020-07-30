/* Copyright (c) 2015, 2016, 2017 Nicira, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <config.h>
#include "lflow.h"
#include "coverage.h"
#include "gchassis.h"
#include "lport.h"
#include "ofctrl.h"
#include "openvswitch/dynamic-string.h"
#include "openvswitch/ofp-actions.h"
#include "openvswitch/ofpbuf.h"
#include "openvswitch/vlog.h"
#include "ovn-controller.h"
#include "ovn/actions.h"
#include "ovn/expr.h"
#include "ovn/lib/ovn-l7.h"
#include "ovn/lib/ovn-sb-idl.h"
#include "ovn/lib/extend-table.h"
#include "packets.h"
#include "physical.h"
#include "simap.h"
#include "sset.h"

VLOG_DEFINE_THIS_MODULE(lflow);

COVERAGE_DEFINE(lflow_run);

/* Symbol table. */

/* Contains "struct expr_symbol"s for fields supported by OVN lflows. */
static struct shash symtab;

void
lflow_init(void)
{
    ovn_init_symtab(&symtab);
}

struct lookup_port_aux {
    struct ovsdb_idl_index *sbrec_multicast_group_by_name_datapath;
    struct ovsdb_idl_index *sbrec_port_binding_by_name;
    const struct sbrec_datapath_binding *dp;
};

struct condition_aux {
    struct ovsdb_idl_index *sbrec_chassis_by_name;
    struct ovsdb_idl_index *sbrec_port_binding_by_name;
    const struct sbrec_chassis *chassis;
    const struct sset *active_tunnels;
};

static void consider_logical_flow(
    struct ovsdb_idl_index *sbrec_chassis_by_name,
    struct ovsdb_idl_index *sbrec_multicast_group_by_name_datapath,
    struct ovsdb_idl_index *sbrec_port_binding_by_name,
    const struct sbrec_logical_flow *,
    const struct hmap *local_datapaths,
    const struct sbrec_chassis *,
    struct hmap *dhcp_opts,
    struct hmap *dhcpv6_opts,
    struct hmap *nd_ra_opts,
    const struct shash *addr_sets,
    const struct shash *port_groups,
    const struct sset *active_tunnels,
    const struct sset *local_lport_ids,
    uint32_t *conj_id_ofs,
    struct hmap *flow_table,
    struct ovn_extend_table *group_table,
    struct ovn_extend_table *meter_table);

static bool
lookup_port_cb(const void *aux_, const char *port_name, unsigned int *portp)
{
    const struct lookup_port_aux *aux = aux_;

    const struct sbrec_port_binding *pb
        = lport_lookup_by_name(aux->sbrec_port_binding_by_name, port_name);
    if (pb && pb->datapath == aux->dp) {
        *portp = pb->tunnel_key;
        return true;
    }

    const struct sbrec_multicast_group *mg = mcgroup_lookup_by_dp_name(
        aux->sbrec_multicast_group_by_name_datapath, aux->dp, port_name);
    if (mg) {
        *portp = mg->tunnel_key;
        return true;
    }

    return false;
}

static bool
is_chassis_resident_cb(const void *c_aux_, const char *port_name)
{
    const struct condition_aux *c_aux = c_aux_;

    const struct sbrec_port_binding *pb
        = lport_lookup_by_name(c_aux->sbrec_port_binding_by_name, port_name);
    if (!pb) {
        return false;
    }
    if (strcmp(pb->type, "chassisredirect")) {
        /* for non-chassisredirect ports */
        return pb->chassis && pb->chassis == c_aux->chassis;
    } else {
        struct ovs_list *gateway_chassis;
        gateway_chassis = gateway_chassis_get_ordered(
            c_aux->sbrec_chassis_by_name, pb);
        if (gateway_chassis) {
            bool active = gateway_chassis_is_active(gateway_chassis,
                                                    c_aux->chassis,
                                                    c_aux->active_tunnels);
            gateway_chassis_destroy(gateway_chassis);
            return active;
        }
        return false;
    }
}

static bool
is_switch(const struct sbrec_datapath_binding *ldp)
{
    return smap_get(&ldp->external_ids, "logical-switch") != NULL;

}

/* Adds the logical flows from the Logical_Flow table to flow tables. */
static void
add_logical_flows(
    struct ovsdb_idl_index *sbrec_chassis_by_name,
    struct ovsdb_idl_index *sbrec_multicast_group_by_name_datapath,
    struct ovsdb_idl_index *sbrec_port_binding_by_name,
    const struct sbrec_dhcp_options_table *dhcp_options_table,
    const struct sbrec_dhcpv6_options_table *dhcpv6_options_table,
    const struct sbrec_logical_flow_table *logical_flow_table,
    const struct hmap *local_datapaths,
    const struct sbrec_chassis *chassis,
    const struct shash *addr_sets,
    const struct shash *port_groups,
    const struct sset *active_tunnels,
    const struct sset *local_lport_ids,
    struct hmap *flow_table,
    struct ovn_extend_table *group_table,
    struct ovn_extend_table *meter_table)
{
    uint32_t conj_id_ofs = 1;
    const struct sbrec_logical_flow *lflow;

    struct hmap dhcp_opts = HMAP_INITIALIZER(&dhcp_opts);
    struct hmap dhcpv6_opts = HMAP_INITIALIZER(&dhcpv6_opts);
    const struct sbrec_dhcp_options *dhcp_opt_row;
    SBREC_DHCP_OPTIONS_TABLE_FOR_EACH (dhcp_opt_row, dhcp_options_table) {
        dhcp_opt_add(&dhcp_opts, dhcp_opt_row->name, dhcp_opt_row->code,
                     dhcp_opt_row->type);
    }


    const struct sbrec_dhcpv6_options *dhcpv6_opt_row;
    SBREC_DHCPV6_OPTIONS_TABLE_FOR_EACH (dhcpv6_opt_row,
                                         dhcpv6_options_table) {
       dhcp_opt_add(&dhcpv6_opts, dhcpv6_opt_row->name, dhcpv6_opt_row->code,
                    dhcpv6_opt_row->type);
    }

    struct hmap nd_ra_opts = HMAP_INITIALIZER(&nd_ra_opts);
    nd_ra_opts_init(&nd_ra_opts);

    SBREC_LOGICAL_FLOW_TABLE_FOR_EACH (lflow, logical_flow_table) {
        consider_logical_flow(sbrec_chassis_by_name,
                              sbrec_multicast_group_by_name_datapath,
                              sbrec_port_binding_by_name,
                              lflow, local_datapaths,
                              chassis, &dhcp_opts, &dhcpv6_opts, &nd_ra_opts,
                              addr_sets, port_groups, active_tunnels,
                              local_lport_ids, &conj_id_ofs,
                              flow_table, group_table, meter_table);
    }

    dhcp_opts_destroy(&dhcp_opts);
    dhcp_opts_destroy(&dhcpv6_opts);
    nd_ra_opts_destroy(&nd_ra_opts);
}

static void
consider_logical_flow(
    struct ovsdb_idl_index *sbrec_chassis_by_name,
    struct ovsdb_idl_index *sbrec_multicast_group_by_name_datapath,
    struct ovsdb_idl_index *sbrec_port_binding_by_name,
    const struct sbrec_logical_flow *lflow,
    const struct hmap *local_datapaths,
    const struct sbrec_chassis *chassis,
    struct hmap *dhcp_opts,
    struct hmap *dhcpv6_opts,
    struct hmap *nd_ra_opts,
    const struct shash *addr_sets,
    const struct shash *port_groups,
    const struct sset *active_tunnels,
    const struct sset *local_lport_ids,
    uint32_t *conj_id_ofs,
    struct hmap *flow_table,
    struct ovn_extend_table *group_table,
    struct ovn_extend_table *meter_table)
{
    /* Determine translation of logical table IDs to physical table IDs. */
    bool ingress = !strcmp(lflow->pipeline, "ingress");

    const struct sbrec_datapath_binding *ldp = lflow->logical_datapath;
    if (!ldp) {
        return;
    }
    if (!get_local_datapath(local_datapaths, ldp->tunnel_key)) {
        return;
    }

    /* Determine translation of logical table IDs to physical table IDs. */
    uint8_t first_ptable = (ingress
                            ? OFTABLE_LOG_INGRESS_PIPELINE
                            : OFTABLE_LOG_EGRESS_PIPELINE);
    uint8_t ptable = first_ptable + lflow->table_id;
    uint8_t output_ptable = (ingress
                             ? OFTABLE_REMOTE_OUTPUT
                             : OFTABLE_SAVE_INPORT);

    /* Parse OVN logical actions.
     *
     * XXX Deny changes to 'outport' in egress pipeline. */
    uint64_t ovnacts_stub[1024 / 8];
    struct ofpbuf ovnacts = OFPBUF_STUB_INITIALIZER(ovnacts_stub);
    struct ovnact_parse_params pp = {
        .symtab = &symtab,
        .dhcp_opts = dhcp_opts,
        .dhcpv6_opts = dhcpv6_opts,
        .nd_ra_opts = nd_ra_opts,

        .pipeline = ingress ? OVNACT_P_INGRESS : OVNACT_P_EGRESS,
        .n_tables = LOG_PIPELINE_LEN,
        .cur_ltable = lflow->table_id,
    };
    struct expr *prereqs;
    char *error;

    error = ovnacts_parse_string(lflow->actions, &pp, &ovnacts, &prereqs);
    if (error) {
        static struct vlog_rate_limit rl = VLOG_RATE_LIMIT_INIT(1, 1);
        VLOG_WARN_RL(&rl, "error parsing actions \"%s\": %s",
                     lflow->actions, error);
        free(error);
        ovnacts_free(ovnacts.data, ovnacts.size);
        ofpbuf_uninit(&ovnacts);
        return;
    }

    /* Translate OVN match into table of OpenFlow matches. */
    struct hmap matches;
    struct expr *expr;

    expr = expr_parse_string(lflow->match, &symtab, addr_sets, port_groups,
                             &error);
    if (!error) {
        if (prereqs) {
            expr = expr_combine(EXPR_T_AND, expr, prereqs);
            prereqs = NULL;
        }
        expr = expr_annotate(expr, &symtab, &error);
    }
    if (error) {
        static struct vlog_rate_limit rl = VLOG_RATE_LIMIT_INIT(1, 1);
        VLOG_WARN_RL(&rl, "error parsing match \"%s\": %s",
                     lflow->match, error);
        expr_destroy(prereqs);
        free(error);
        ovnacts_free(ovnacts.data, ovnacts.size);
        ofpbuf_uninit(&ovnacts);
        return;
    }

    struct lookup_port_aux aux = {
        .sbrec_multicast_group_by_name_datapath
            = sbrec_multicast_group_by_name_datapath,
        .sbrec_port_binding_by_name = sbrec_port_binding_by_name,
        .dp = lflow->logical_datapath
    };
    struct condition_aux cond_aux = {
        .sbrec_chassis_by_name = sbrec_chassis_by_name,
        .sbrec_port_binding_by_name = sbrec_port_binding_by_name,
        .chassis = chassis,
        .active_tunnels = active_tunnels,
    };
    expr = expr_simplify(expr, is_chassis_resident_cb, &cond_aux);
    expr = expr_normalize(expr);
    uint32_t n_conjs = expr_to_matches(expr, lookup_port_cb, &aux,
                                       &matches);
    expr_destroy(expr);

    if (hmap_is_empty(&matches)) {
        ovnacts_free(ovnacts.data, ovnacts.size);
        ofpbuf_uninit(&ovnacts);
        expr_matches_destroy(&matches);
        return;
    }

    /* Encode OVN logical actions into OpenFlow. */
    uint64_t ofpacts_stub[1024 / 8];
    struct ofpbuf ofpacts = OFPBUF_STUB_INITIALIZER(ofpacts_stub);
    struct ovnact_encode_params ep = {
        .lookup_port = lookup_port_cb,
        .aux = &aux,
        .is_switch = is_switch(ldp),
        .group_table = group_table,
        .meter_table = meter_table,

        .pipeline = ingress ? OVNACT_P_INGRESS : OVNACT_P_EGRESS,
        .ingress_ptable = OFTABLE_LOG_INGRESS_PIPELINE,
        .egress_ptable = OFTABLE_LOG_EGRESS_PIPELINE,
        .output_ptable = output_ptable,
        .mac_bind_ptable = OFTABLE_MAC_BINDING,
    };
    ovnacts_encode(ovnacts.data, ovnacts.size, &ep, &ofpacts);
    ovnacts_free(ovnacts.data, ovnacts.size);
    ofpbuf_uninit(&ovnacts);

    /* Prepare the OpenFlow matches for adding to the flow table. */
    struct expr_match *m;
    HMAP_FOR_EACH (m, hmap_node, &matches) {
        match_set_metadata(&m->match,
                           htonll(lflow->logical_datapath->tunnel_key));
        if (m->match.wc.masks.conj_id) {
            m->match.flow.conj_id += *conj_id_ofs;
        }
        if (is_switch(ldp)) {
            unsigned int reg_index
                = (ingress ? MFF_LOG_INPORT : MFF_LOG_OUTPORT) - MFF_REG0;
            int64_t port_id = m->match.flow.regs[reg_index];
            if (port_id) {
                int64_t dp_id = lflow->logical_datapath->tunnel_key;
                char buf[16];
                snprintf(buf, sizeof(buf), "%"PRId64"_%"PRId64, dp_id, port_id);
                if (!sset_contains(local_lport_ids, buf)) {
                    continue;
                }
            }
        }
        if (!m->n) {
            ofctrl_add_flow(flow_table, ptable, lflow->priority,
                            lflow->header_.uuid.parts[0], &m->match, &ofpacts);
        } else {
            uint64_t conj_stubs[64 / 8];
            struct ofpbuf conj;

            ofpbuf_use_stub(&conj, conj_stubs, sizeof conj_stubs);
            for (int i = 0; i < m->n; i++) {
                const struct cls_conjunction *src = &m->conjunctions[i];
                struct ofpact_conjunction *dst;

                dst = ofpact_put_CONJUNCTION(&conj);
                dst->id = src->id + *conj_id_ofs;
                dst->clause = src->clause;
                dst->n_clauses = src->n_clauses;
            }
            ofctrl_add_flow(flow_table, ptable, lflow->priority, 0, &m->match,
                            &conj);
            ofpbuf_uninit(&conj);
        }
    }

    /* Clean up. */
    expr_matches_destroy(&matches);
    ofpbuf_uninit(&ofpacts);
    *conj_id_ofs += n_conjs;
}

static void
put_load(const uint8_t *data, size_t len,
         enum mf_field_id dst, int ofs, int n_bits,
         struct ofpbuf *ofpacts)
{
    struct ofpact_set_field *sf = ofpact_put_set_field(ofpacts,
                                                       mf_from_id(dst), NULL,
                                                       NULL);
    bitwise_copy(data, len, 0, sf->value, sf->field->n_bytes, ofs, n_bits);
    bitwise_one(ofpact_set_field_mask(sf), sf->field->n_bytes, ofs, n_bits);
}

static void
consider_neighbor_flow(struct ovsdb_idl_index *sbrec_port_binding_by_name,
                       const struct sbrec_mac_binding *b,
                       struct hmap *flow_table)
{
    const struct sbrec_port_binding *pb
        = lport_lookup_by_name(sbrec_port_binding_by_name, b->logical_port);
    if (!pb) {
        return;
    }

    struct eth_addr mac;
    if (!eth_addr_from_string(b->mac, &mac)) {
        static struct vlog_rate_limit rl = VLOG_RATE_LIMIT_INIT(5, 1);
        VLOG_WARN_RL(&rl, "bad 'mac' %s", b->mac);
        return;
    }

    struct match match = MATCH_CATCHALL_INITIALIZER;
    if (strchr(b->ip, '.')) {
        ovs_be32 ip;
        if (!ip_parse(b->ip, &ip)) {
            static struct vlog_rate_limit rl = VLOG_RATE_LIMIT_INIT(5, 1);
            VLOG_WARN_RL(&rl, "bad 'ip' %s", b->ip);
            return;
        }
        match_set_reg(&match, 0, ntohl(ip));
    } else {
        struct in6_addr ip6;
        if (!ipv6_parse(b->ip, &ip6)) {
            static struct vlog_rate_limit rl = VLOG_RATE_LIMIT_INIT(5, 1);
            VLOG_WARN_RL(&rl, "bad 'ip' %s", b->ip);
            return;
        }
        ovs_be128 value;
        memcpy(&value, &ip6, sizeof(value));
        match_set_xxreg(&match, 0, ntoh128(value));
    }

    match_set_metadata(&match, htonll(pb->datapath->tunnel_key));
    match_set_reg(&match, MFF_LOG_OUTPORT - MFF_REG0, pb->tunnel_key);

    uint64_t stub[1024 / 8];
    struct ofpbuf ofpacts = OFPBUF_STUB_INITIALIZER(stub);
    put_load(mac.ea, sizeof mac.ea, MFF_ETH_DST, 0, 48, &ofpacts);
    ofctrl_add_flow(flow_table, OFTABLE_MAC_BINDING, 100, 0, &match, &ofpacts);
    ofpbuf_uninit(&ofpacts);
}

/* Adds an OpenFlow flow to flow tables for each MAC binding in the OVN
 * southbound database. */
static void
add_neighbor_flows(struct ovsdb_idl_index *sbrec_port_binding_by_name,
                   const struct sbrec_mac_binding_table *mac_binding_table,
                   struct hmap *flow_table)
{
    const struct sbrec_mac_binding *b;
    SBREC_MAC_BINDING_TABLE_FOR_EACH (b, mac_binding_table) {
        consider_neighbor_flow(sbrec_port_binding_by_name, b, flow_table);
    }
}

/* Translates logical flows in the Logical_Flow table in the OVN_SB database
 * into OpenFlow flows.  See ovn-architecture(7) for more information. */
void
lflow_run(struct ovsdb_idl_index *sbrec_chassis_by_name,
          struct ovsdb_idl_index *sbrec_multicast_group_by_name_datapath,
          struct ovsdb_idl_index *sbrec_port_binding_by_name,
          const struct sbrec_dhcp_options_table *dhcp_options_table,
          const struct sbrec_dhcpv6_options_table *dhcpv6_options_table,
          const struct sbrec_logical_flow_table *logical_flow_table,
          const struct sbrec_mac_binding_table *mac_binding_table,
          const struct sbrec_chassis *chassis,
          const struct hmap *local_datapaths,
          const struct shash *addr_sets,
          const struct shash *port_groups,
          const struct sset *active_tunnels,
          const struct sset *local_lport_ids,
          struct hmap *flow_table,
          struct ovn_extend_table *group_table,
          struct ovn_extend_table *meter_table)
{
    COVERAGE_INC(lflow_run);

    add_logical_flows(sbrec_chassis_by_name,
                      sbrec_multicast_group_by_name_datapath,
                      sbrec_port_binding_by_name, dhcp_options_table,
                      dhcpv6_options_table, logical_flow_table,
                      local_datapaths, chassis, addr_sets, port_groups,
                      active_tunnels, local_lport_ids, flow_table, group_table,
                      meter_table);
    add_neighbor_flows(sbrec_port_binding_by_name, mac_binding_table,
                       flow_table);
}

void
lflow_destroy(void)
{
    expr_symtab_destroy(&symtab);
    shash_destroy(&symtab);
}
