/* Copyright (c) 2015, 2016 Nicira, Inc.
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

#ifndef OVN_LFLOW_H
#define OVN_LFLOW_H 1

#include "ovn/lib/logical-fields.h"

/* Logical_Flow table translation to OpenFlow
 * ==========================================
 *
 * The Logical_Flow table obtained from the OVN_Southbound database works in
 * terms of logical entities, that is, logical flows among logical datapaths
 * and logical ports.  This code translates these logical flows into OpenFlow
 * flows that, again, work in terms of logical entities implemented through
 * OpenFlow extensions (e.g. registers represent the logical input and output
 * ports).
 *
 * Physical-to-logical and logical-to-physical translation are implemented in
 * physical.[ch] as separate OpenFlow tables that run before and after,
 * respectively, the logical pipeline OpenFlow tables.
 */

#include <stdint.h>

struct ovn_extend_table;
struct ovsdb_idl_index;
struct hmap;
struct sbrec_chassis;
struct sbrec_dhcp_options_table;
struct sbrec_dhcpv6_options_table;
struct sbrec_logical_flow_table;
struct sbrec_mac_binding_table;
struct simap;
struct sset;
struct uuid;

/* OpenFlow table numbers.
 *
 * These are heavily documented in ovn-architecture(7), please update it if
 * you make any changes. */
#define OFTABLE_PHY_TO_LOG            0
#define OFTABLE_LOG_INGRESS_PIPELINE  8 /* First of LOG_PIPELINE_LEN tables. */
#define OFTABLE_REMOTE_OUTPUT        32
#define OFTABLE_LOCAL_OUTPUT         33
#define OFTABLE_CHECK_LOOPBACK       34
#define OFTABLE_LOG_EGRESS_PIPELINE  40 /* First of LOG_PIPELINE_LEN tables. */
#define OFTABLE_SAVE_INPORT          64
#define OFTABLE_LOG_TO_PHY           65
#define OFTABLE_MAC_BINDING          66

/* The number of tables for the ingress and egress pipelines. */
#define LOG_PIPELINE_LEN 24

void lflow_init(void);
void lflow_run(struct ovsdb_idl_index *sbrec_chassis_by_name,
               struct ovsdb_idl_index *sbrec_multicast_group_by_name_datapath,
               struct ovsdb_idl_index *sbrec_port_binding_by_name,
               const struct sbrec_dhcp_options_table *,
               const struct sbrec_dhcpv6_options_table *,
               const struct sbrec_logical_flow_table *,
               const struct sbrec_mac_binding_table *,
               const struct sbrec_chassis *chassis,
               const struct hmap *local_datapaths,
               const struct shash *addr_sets,
               const struct shash *port_groups,
               const struct sset *active_tunnels,
               const struct sset *local_lport_ids,
               struct hmap *flow_table,
               struct ovn_extend_table *group_table,
               struct ovn_extend_table *meter_table);
void lflow_destroy(void);

#endif /* ovn/lflow.h */
