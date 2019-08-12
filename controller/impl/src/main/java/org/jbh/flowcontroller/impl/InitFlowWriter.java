package org.jbh.flowcontroller.impl;

import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;


public class InitFlowWriter implements DataTreeChangeListener<Node> {
    private final DataBroker dataBroker;
    private final SalFlowService salFlowService;

    private ListenerRegistration nodeRegistration;
    private final ExecutorService initialFlowExecutor = Executors.newCachedThreadPool();

    private short flowTableId = 0;
    private int flowPriority = 0;
    private int flowHardTimeout = 0;
    private int flowIdleTimeout = 0;
    private static final String FLOW_ID_PREFIX = "flowTest-";
    private final AtomicLong flowIdInc = new AtomicLong();
    private final AtomicLong flowCookieInc = new AtomicLong(0x2b00000000000000L);

    public InitFlowWriter(DataBroker dataBroker, SalFlowService salFlowService){
        this.dataBroker = dataBroker;
        this.salFlowService = salFlowService;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        Set<InstanceIdentifier<?>> nodeIds = new HashSet<>();
        for (DataTreeModification<Node> change: changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            final InstanceIdentifier<Node> identifier = change.getRootPath().getRootIdentifier();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    if (rootNode.getDataBefore() == null) {
                        nodeIds.add(identifier);
                    }
                    break;
                default:
                    break;
            }
        }

        if (!nodeIds.isEmpty()) {
            initialFlowExecutor.execute(new InitialFlowWriterProcessor(nodeIds));
        }
    }

    private class InitialFlowWriterProcessor implements Runnable{
        private final Set<InstanceIdentifier<?>> nodeIds;

        InitialFlowWriterProcessor(Set<InstanceIdentifier<?>> nodeIds) {
            this.nodeIds = nodeIds;
        }

        @Override
        public void run() {
            // 下流表
            for(InstanceIdentifier nodeId : nodeIds){
                if (Node.class.isAssignableFrom(nodeId.getTargetType())) {
                    InstanceIdentifier<Node> invNodeId = (InstanceIdentifier<Node>) nodeId;
                    if (invNodeId.firstKeyOf(Node.class, NodeKey.class).getId().getValue().contains("openflow:")) {
                        addInitialFlows(invNodeId);
                    }
                }
            }
        }

        private void addInitialFlows(InstanceIdentifier<Node> nodeId){
            InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
            InstanceIdentifier<Flow> flowId = getFlowInstanceId(tableId);

            //add send all flow
            writeFlowToController(nodeId, tableId, flowId, createSendAllToControllerFlow(flowTableId, flowPriority));
        }

        private InstanceIdentifier<Table> getTableInstanceId(InstanceIdentifier<Node> nodeId) {
            // get flow table key
            TableKey flowTableKey = new TableKey(flowTableId);
            return nodeId.builder()
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class, flowTableKey)
                    .build();
        }

        private InstanceIdentifier<Flow> getFlowInstanceId(InstanceIdentifier<Table> tableId) {
            // generate unique flow key
            FlowId flowId = new FlowId(FLOW_ID_PREFIX + String.valueOf(flowIdInc.getAndIncrement()));
            FlowKey flowKey = new FlowKey(flowId);
            return tableId.child(Flow.class, flowKey);
        }

        private Flow createSendAllToControllerFlow(Short tableId, int priority) {
            // start building flow
            FlowBuilder sendAll = new FlowBuilder() //
                    .setTableId(tableId) //
                    .setFlowName("sendall");
            // use its own hash code for id.
            sendAll.setId(new FlowId(Long.toString(sendAll.hashCode())));

            //Match不设置
            Match match = new MatchBuilder().build();

            //Action
            Action sendToController = getSendToControllerAction();

            // Create an Apply Action
            ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(sendToController))
                    .build();

            // Wrap our Apply Action in an Instruction
            Instruction applyActionsInstruction = new InstructionBuilder() //
                    .setOrder(0)
                    .setInstruction(new ApplyActionsCaseBuilder()//
                            .setApplyActions(applyActions) //
                            .build()) //
                    .build();

            // Put our Instruction in a list of Instructions
            sendAll
                    .setMatch(match) //
                    .setInstructions(new InstructionsBuilder() //
                            .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                            .build()) //
                    .setPriority(priority) //
                    .setBufferId(0xffffffffL) //
                    .setHardTimeout(flowHardTimeout) //
                    .setIdleTimeout(flowIdleTimeout) //
                    .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                    .setFlags(new FlowModFlags(false, false, false, false, false));

            return sendAll.build();

        }

        private Action getSendToControllerAction(){
            Action sendToController = new ActionBuilder()
                    .setOrder(0)
                    .setKey(new ActionKey(0))
                    .setAction(new OutputActionCaseBuilder()
                            .setOutputAction(new OutputActionBuilder()
                                    .setMaxLength(0xffff)
                                    .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                    .build())
                            .build())
                    .build();
            return sendToController;
        }

        private Future<RpcResult<AddFlowOutput>> writeFlowToController(InstanceIdentifier<Node> nodeInstanceId,
                                                                       InstanceIdentifier<Table> tableInstanceId,
                                                                       InstanceIdentifier<Flow> flowPath,
                                                                       Flow flow) {
            final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(flowPath));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            builder.setTransactionUri(new Uri(flow.getId().getValue()));
            return salFlowService.addFlow(builder.build());
        }
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        //注册DataTreeChangeListener
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();
        DataTreeIdentifier<Node> treeIdentifier = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier);
        nodeRegistration = dataBroker.registerDataTreeChangeListener(treeIdentifier, this);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        nodeRegistration.close();
    }



}
