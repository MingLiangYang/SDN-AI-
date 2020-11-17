package org.jbh.flowcontroller.impl.packethandler.topology;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public interface NetworkGraphService {
    /**
     * Adds links to existing graph or creates new graph with given links if
     * graph was not initialized.
     *
     * @param links the links to add
     */
    void addLinks(List<Link> links);

    /**
     * Removes links from existing graph.
     *
     * @param links the links to remove
     */
    void removeLinks(List<Link> links);

    /**
     * Returns a path between 2 nodes. Implementation should ideally return
     * shortest path.
     *
     * @param sourceNodeId the source node Id
     * @param destinationNodeId the destination node Id
     */
    List<Link> getPath(NodeId sourceNodeId, NodeId destinationNodeId);

    /**
     * Returns all the links in current network graph.
     */
    List<Link> getAllLinks();

    /**
     * Clears the prebuilt graph, in case same service instance is required to
     * process a new graph.
     */
    void clear();

}
