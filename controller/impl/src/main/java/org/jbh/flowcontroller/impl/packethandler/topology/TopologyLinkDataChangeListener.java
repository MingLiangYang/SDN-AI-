package org.jbh.flowcontroller.impl.packethandler.topology;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens to data change events on topology links {@link
 * org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
 * and maintains a topology graph using provided NetworkGraphImpl
 * {@link org.jbh.flowcontroller.impl.packethandler.topology.NetworkGraphImpl}
 * and maintains a host-to-switchPort vision.
 *
 * It refreshes the graph after a delay(default 1 sec) to accommodate burst of
 * change events if they come in bulk. This is to avoid continuous refresh of
 * graph on a series of change events in short time.
 */
public class TopologyLinkDataChangeListener implements DataTreeChangeListener<Link> {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyLinkDataChangeListener.class);

    private final DataBroker dataBroker;
    private NetworkGraphService networkGraphService;
    private final HashMap<String, String> hostToSwitchTP;

    private String topologyId = "flow:1";
    private long graphRefreshDelay = 1000;                  // 1s内没变化 才会更新图
    // 已经启动延迟更新任务
    private boolean networkGraphRefreshScheduled = false;
    // 长度为1的 scheduled thread pool
    private final ScheduledExecutorService topologyDataChangeEventProcessor = Executors.newScheduledThreadPool(1);
    // 启动延迟过程中 拓扑频繁变化 重新延迟
    private boolean threadReschedule = false;

    public TopologyLinkDataChangeListener(final DataBroker dataBroker, final NetworkGraphService networkGraphService
            , HashMap<String, String> hostToSwitchTP){
        this.dataBroker = dataBroker;
        this.networkGraphService = networkGraphService;
        this.hostToSwitchTP = hostToSwitchTP;
    }

    /**
     * Handler for onDataChanged events and schedules the building of the
     * network graph.
     */
    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Link>> changes) {
        boolean isGraphUpdated = false;
        for (DataTreeModification<Link> change: changes) {
            DataObjectModification<Link> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    Link createdLink = rootNode.getDataAfter();
                    if (rootNode.getDataBefore() == null) {
                        isGraphUpdated = true;
                        LOG.debug("JBH: Graph is updated! Added Link {}", createdLink.getLinkId().getValue());
                    }
                    break;
                case DELETE:
                    Link deletedLink = rootNode.getDataBefore();
                    isGraphUpdated = true;
                    LOG.debug("JBH: Graph is updated! Removed Link {}", deletedLink.getLinkId().getValue());
                    break;
                default:
                    break;
            }
        }
        if (!isGraphUpdated) {
            return;
        }
        if (!networkGraphRefreshScheduled) {
            synchronized (this) {
                if (!networkGraphRefreshScheduled) {
                    topologyDataChangeEventProcessor.schedule(new TopologyDataChangeEventProcessor(), graphRefreshDelay,
                            TimeUnit.MILLISECONDS);
                    networkGraphRefreshScheduled = true;
                }
            }
        } else{
            threadReschedule = true;
        }

    }

    /**
     * Registers as a data listener to receive changes done to {@link org.opendaylight.yang.gen.v1.urn.tbd.params
     * .xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
     * under
     * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
     * operation data root.
     */
    public ListenerRegistration<TopologyLinkDataChangeListener> registerAsDataChangeListener() {
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId))).child(Link.class).build();
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, linkInstance), this);
    }

    private class TopologyDataChangeEventProcessor implements Runnable {
        /**
         * 初始化networkGraphService 和 hostToSwitchTP
         * 读取datastore
         * 写networkGraphService 和 hostToSwitchTP
         */
        @Override
        public void run() {
            if (threadReschedule){
                topologyDataChangeEventProcessor.schedule(this, graphRefreshDelay, TimeUnit.MILLISECONDS);
                threadReschedule = false;
                return;
            }
            LOG.debug("In network graph refresh thread.");
            networkGraphRefreshScheduled = false;
            networkGraphService.clear();
            hostToSwitchTP.clear();
            List<Link> hostLinks = new ArrayList<>();
            List<Link> links = getLinksFromTopology(hostLinks);
            if (links == null || links.isEmpty()) {
                return;
            }
            networkGraphService.addLinks(links);
            updateHostToSwitch(hostLinks);
            LOG.debug("Done with network graph refresh thread.");
        }

        /**
         *
         * @return topo "flow:1"'s all switch link, and hostLinks
         */
        private List<Link> getLinksFromTopology(List<Link> hostLinks) {
            InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId(topologyId))).build();

            Topology topology = null;
            ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            try {
                Optional<Topology> topologyOptional = readOnlyTransaction
                        .read(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier).get();
                if (topologyOptional.isPresent()) {
                    topology = topologyOptional.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                readOnlyTransaction.close();
                throw new RuntimeException(
                        "Error reading from operational store, topology : " + topologyInstanceIdentifier, e);
            }
            readOnlyTransaction.close();
            if (topology == null) {
                return null;
            }
            List<Link> links = topology.getLink();
            if (links == null || links.isEmpty()) {
                return null;
            }
            List<Link> internalLinks = new ArrayList<>();
            for (Link link : links) {
                if (!link.getLinkId().getValue().contains("host")) {
                    internalLinks.add(link);
                } else {
                    hostLinks.add(link);
                }
            }
            LOG.debug("JBH: In getLinksFromTopology: internalLinks:{}",internalLinks);
            LOG.debug("JBH: In getLinksFromTopology: hostLinks:{}",hostLinks);
            return internalLinks;
        }

        /**
         * 根据带有host名字的Links，更新host-to-switchPort视图
         */
        private void updateHostToSwitch(List<Link> hostLinks){
            for(Link link:hostLinks){
                String node1 = link.getSource().getSourceNode().getValue();
                String node2 = link.getDestination().getDestNode().getValue();
                if(node1.contains("host")){    //node1 is "host:b6:16:2b:b1:95:7d" node2 is "openflow:1"
                    if(!hostToSwitchTP.containsKey(node1)){
                        hostToSwitchTP.put(node1,link.getDestination().getDestTp().getValue());
                    }
                }else if(node2.contains("host")){
                    if(!hostToSwitchTP.containsKey(node2)){
                        hostToSwitchTP.put(node2,link.getSource().getSourceTp().getValue());
                    }
                }
            }
        }
    }
}
