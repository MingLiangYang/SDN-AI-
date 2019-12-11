package org.jbh.flowcontroller.impl.packethandler.topology;

import com.google.common.base.Preconditions;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;


/**
 * Implementation of NetworkGraphService
 * {@link org.jbh.flowcontroller.impl.packethandler.topology.NetworkGraphService}. It
 * uses Jung graph library internally to maintain a graph and optimum way to
 * return shortest path using Dijkstra algorithm.
 */
public class NetworkGraphImpl implements NetworkGraphService {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkGraphImpl.class);

    @GuardedBy("this")
    private Graph<NodeId, Link> networkGraph;
    private final Set<String> linkAdded = new HashSet<>();

    DijkstraShortestPath<NodeId, Link> shortestPath = null;

    /**
     * Adds links to existing graph or creates new graph with given links if
     * graph was not initialized.
     *
     * @param links the links to add
     */
    @Override
    public synchronized void addLinks(List<Link> links){
        if (links == null || links.isEmpty()) {
            LOG.info("In addLinks: No link added as links is null or empty.");
            return;
        }

        if (networkGraph == null) {
            networkGraph = SparseMultigraph.<NodeId, Link>getFactory().get();
        }

        for (Link link : links) {
            if (linkAlreadyAdded(link)) {
                continue;
            }
            NodeId sourceNodeId = link.getSource().getSourceNode();
            NodeId destinationNodeId = link.getDestination().getDestNode();
            networkGraph.addVertex(sourceNodeId);
            networkGraph.addVertex(destinationNodeId);
            networkGraph.addEdge(link, sourceNodeId, destinationNodeId, EdgeType.UNDIRECTED);
        }


        if(shortestPath == null) {
            shortestPath = new DijkstraShortestPath<>(networkGraph);
        }
        else {
            shortestPath.reset();
        }
    }

    @GuardedBy("this")
    private boolean linkAlreadyAdded(Link link) {
        String linkAddedKey = null;
        if (link.getDestination().getDestTp().hashCode() > link.getSource().getSourceTp().hashCode()) {
            linkAddedKey = link.getSource().getSourceTp().getValue() + link.getDestination().getDestTp().getValue();
        } else {
            linkAddedKey = link.getDestination().getDestTp().getValue() + link.getSource().getSourceTp().getValue();
        }
        if (linkAdded.contains(linkAddedKey)) {
            return true;
        } else {
            linkAdded.add(linkAddedKey);
            return false;
        }
    }

    /**
     * Removes links from existing graph.
     *
     * @param links
     *            The links to remove.
     */
    @Override
    public synchronized void removeLinks(List<Link> links) {
        Preconditions.checkNotNull(networkGraph, "Graph is not initialized, add links first.");

        if (links == null || links.isEmpty()) {
            LOG.info("In removeLinks: No link removed as links is null or empty.");
            return;
        }

        for (Link link : links) {
            networkGraph.removeEdge(link);
        }

        if(shortestPath == null) {
            shortestPath = new DijkstraShortestPath<>(networkGraph);
        } else {
            shortestPath.reset();
        }


    }

    /**
     * returns a path between 2 nodes. Uses Dijkstra's algorithm to return
     * shortest path.
     *
     * @param sourceNodeId the source node Id
     * @param destinationNodeId the destination node Id
     */
    @Override

    public synchronized List<Link> getPath(NodeId sourceNodeId, NodeId destinationNodeId) {
        Preconditions.checkNotNull(shortestPath, "Graph is not initialized, add links first.");
        if(sourceNodeId == null || destinationNodeId == null) {
            LOG.info("In getPath: returning null, as sourceNodeId or destinationNodeId is null.");
            return null;
        }
        return shortestPath.getPath(sourceNodeId, destinationNodeId);
    }


    /**
     * Clears the prebuilt graph, in case same service instance is required to
     * process a new graph.
     */
    @Override
    public synchronized void clear() {
        networkGraph = null;
        linkAdded.clear();
        shortestPath = null;
    }

    /**
     * Get all the links in the network.
     *
     * @return The links in the network.
     */
    @Override
    public synchronized List<Link> getAllLinks() {
        List<Link> allLinks = new ArrayList<>();
        if (networkGraph != null) {
            allLinks.addAll(networkGraph.getEdges());
        }
        return allLinks;
    }
}
