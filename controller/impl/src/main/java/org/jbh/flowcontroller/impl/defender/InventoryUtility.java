package org.jbh.flowcontroller.impl.defender;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;


public class InventoryUtility
{
    private InventoryUtility()
    {

    }

    public static final String OPENFLOW_NODE_PREFIX = "openflow:";

    /**
     * @param nodeConnectorRef
     * @return NodeId getNodeId
     */

    public static NodeId getNodeId(NodeConnectorRef nodeConnectorRef)
    {
        return nodeConnectorRef.getValue()
                .firstKeyOf(Node.class, NodeKey.class)
                .getId();
    }

    /**
     * @param nodeConnectorRef
     * @return NodeConnectorId getNodeConnectorId
     */

    public static NodeConnectorId getNodeConnectorId(NodeConnectorRef nodeConnectorRef)
    {
        return nodeConnectorRef.getValue()// 返回一个Instanceidentifier
                .firstKeyOf(NodeConnector.class, NodeConnectorKey.class)
                .getId();
    }
}
