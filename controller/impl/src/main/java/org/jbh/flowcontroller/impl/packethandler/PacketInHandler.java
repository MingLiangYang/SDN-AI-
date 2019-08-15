package org.jbh.flowcontroller.impl.packethandler;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.MoreExecutors;

import org.jbh.flowcontroller.impl.packethandler.utils.BitBufferHelper;
import org.jbh.flowcontroller.impl.packethandler.utils.BufferException;
import org.jbh.flowcontroller.impl.packethandler.utils.HexEncode;
import org.jbh.flowcontroller.impl.packethandler.utils.NetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.Header8021qType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.fields.Header8021qBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class PacketInHandler implements PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(PacketInHandler.class);

    private NotificationService notificationService;
    private SalFlowService salFlowService;
    private PacketProcessingService packetProcessingService;
    private InventoryReader inventoryReader;

    private ListenerRegistration packetListenerRegistration;

    private short flowTableId = 0;
    private int flowPriority = 10;
    private int flowHardTimeout = 600;
    private int flowIdleTimeout = 300;
    private static final String FLOW_ID_PREFIX = "flowTest-";
    private final AtomicLong flowIdInc = new AtomicLong();
    private final AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);
    final byte[] ETH_TYPE_IPV4 = new byte[] { 0x08, 0x00 };

    public PacketInHandler(DataBroker dataBroker, NotificationService notificationService,
                           SalFlowService salFlowService, PacketProcessingService packetProcessingService){
        this.notificationService = notificationService;
        this.salFlowService = salFlowService;
        this.packetProcessingService = packetProcessingService;
        this.inventoryReader = new InventoryReader(dataBroker);
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        byte[] data = packetReceived.getPayload();
        byte[] etherType = Arrays.copyOfRange(packetReceived.getPayload(), 12, 14);

        // IPv4 traffic only
        if (Arrays.equals(ETH_TYPE_IPV4, etherType)) {

            try{
                MacAddress desMac =
                        new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 0, 48)));
                MacAddress souceMac =
                        new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 48, 48)));

                int payloadOffset = ethernetDecode(data);
                if(payloadOffset == -1) {
                    LOG.info("JBH: In onPacketReceived: payloadOffset is null");
                    return;
                }
                int bitOffset = payloadOffset * NetUtils.NUM_BITS_IN_A_BYTE;
                Ipv4Address desIp = Ipv4Address.getDefaultInstance(
                        InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 128, 32)).getHostAddress());
                Ipv4Address sourceIp = Ipv4Address.getDefaultInstance(
                        InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset + 96, 32)).getHostAddress());

                LOG.debug("JBH: In onPacketReceived: get a IP Packet, souIP:{}, desIP:{}",sourceIp.getValue(),desIp.getValue());

                NodeConnectorRef ingress = packetReceived.getIngress();
                NodeConnectorRef destNodeConnector = inventoryReader
                        .getNodeConnector(ingress.getValue().firstIdentifierOf(Node.class), desMac);

                if (destNodeConnector != null) {
                    sendPacketOut(packetReceived.getPayload(), ingress, destNodeConnector);
                    addBidirectionalMacToMacFlows(souceMac, ingress, desMac, destNodeConnector, sourceIp, desIp);
                }else{
                    LOG.debug("JBH: In onPacketReceived: destNodeConnector is null. Use default port. ");
                    defaultAction(ingress, sourceIp, desIp, packetReceived.getPayload(), souceMac, desMac);
                }

            }catch (UnknownHostException e){
                LOG.error("JBH: In onPacketReceived: Exception:{} during decoding raw packet to ethernet.", e);
            }catch (BufferException e){
                LOG.error("JBH: In onPacketReceived: Exception:{} during decoding raw packet to ethernet.", e);
            }


        }

    }

    /**新加默认逻辑:
     * 如果目的 MAC 没有端口映射
     * 如果目的IP 不是广播和组播地址
     * Send default PacketOut and addBidrectionalFlow (交换机98从port1出去 交换机23从port4出去) 前提入端口不等于出端口
     *
     */
    private void defaultAction(NodeConnectorRef ingress, Ipv4Address sourceIp, Ipv4Address desIp, byte[] payload
            , MacAddress sourceMac, MacAddress desMac){
        //去掉广播/组播包
        if (ingress == null || desIp.getValue().equals("255.255.255.255") || desIp.getValue().substring(0, 3).equals("224")
                || desIp.getValue().substring(0, 3).equals("225")
                || desIp.getValue().substring(desIp.getValue().length() -3).equals("255")) {
            return;
        }

        String datapath = ingress.getValue().firstIdentifierOf(Node.class)    //"openflow:123"
                .firstKeyOf(Node.class, NodeKey.class).getId().getValue();

        if(datapath.equals("openflow:8796751454798")){
            //如果是交换机1 从port1出去
            NodeConnectorRef egress = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class))
                    .child(NodeConnector.class,
                            new NodeConnectorKey(new NodeConnectorId(datapath+":1")))
                    .build());
            if(egress.equals(ingress)){
                LOG.info("JBH: In defaultAction: Source and Destination ports are same. Ingress:openflow:8796751454798:1 sourceIP:{} desIP:{} sourceMac:{} desMac:{}",
                        sourceIp.getValue(),desIp.getValue(),sourceMac.getValue(),desMac.getValue());
                return;
            }
            sendPacketOut(payload, ingress, egress);
            addBidirectionalMacToMacFlows(sourceMac, ingress, desMac, egress, sourceIp, desIp);
        }else if(datapath.equals("openflow:8796749113023")){
            //如果是交换机2 从port4出去
            NodeConnectorRef egress = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class))
                    .child(NodeConnector.class,
                            new NodeConnectorKey(new NodeConnectorId(datapath+":4")))
                    .build());
            if(egress.equals(ingress)){
                LOG.info("JBH: In defaultAction: Source and Destination ports are same. Ingress:openflow:8796749113023:4 sourceIP:{} desIP:{} sourceMac:{} desMac:{}",
                        sourceIp.getValue(),desIp.getValue(),sourceMac.getValue(),desMac.getValue());
                return;
            }
            sendPacketOut(payload, ingress, egress);
            addBidirectionalMacToMacFlows(sourceMac, ingress, desMac, egress, sourceIp, desIp);
        }else if(datapath.equals("openflow:8796749338201")){
            //如果是交换机01 从port1 出去
            NodeConnectorRef egress = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class))
                    .child(NodeConnector.class,
                            new NodeConnectorKey(new NodeConnectorId(datapath+":1")))
                    .build());
            if(egress.equals(ingress)){
                LOG.info("JBH: In defaultAction: Source and Destination ports are same. Ingress:openflow:8796749338201:1 sourceIP:{} desIP:{} sourceMac:{} desMac:{}",
                        sourceIp.getValue(),desIp.getValue(),sourceMac.getValue(),desMac.getValue());
                return;
            }
            sendPacketOut(payload, ingress, egress);
            addBidirectionalMacToMacFlows(sourceMac, ingress, desMac, egress, sourceIp, desIp);
        }else if(datapath.equals("openflow:8796748406413")){
            //如果是交换机13 从port1 出去
            NodeConnectorRef egress = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, ingress.getValue().firstIdentifierOf(Node.class).firstKeyOf(Node.class, NodeKey.class))
                    .child(NodeConnector.class,
                            new NodeConnectorKey(new NodeConnectorId(datapath+":1")))
                    .build());
            if(egress.equals(ingress)){
                LOG.info("JBH: In defaultAction: Source and Destination ports are same. Ingress:openflow:8796748406413:1 sourceIP:{} desIP:{} sourceMac:{} desMac:{}",
                        sourceIp.getValue(),desIp.getValue(),sourceMac.getValue(),desMac.getValue());
                return;
            }
            sendPacketOut(payload, ingress, egress);
            addBidirectionalMacToMacFlows(sourceMac, ingress, desMac, egress, sourceIp, desIp);
        }
    }

    /**
     * 下双向流表
     * 源mac 目的mac 源ip 目的ip 目的端口
     *
     */
    private void addBidirectionalMacToMacFlows(MacAddress souceMac, NodeConnectorRef ingress, MacAddress desMac
            , NodeConnectorRef destNodeConnector, Ipv4Address sourceIp, Ipv4Address desIp){
        Preconditions.checkNotNull(souceMac, "Source mac address should not be null.");
        Preconditions.checkNotNull(ingress, "inport should not be null.");
        Preconditions.checkNotNull(desMac, "Destination mac address should not be null.");
        Preconditions.checkNotNull(destNodeConnector, "outport should not be null.");
        Preconditions.checkNotNull(sourceIp, "Source ip address should not be null.");
        Preconditions.checkNotNull(desIp, "Destination ip address should not be null.");

        if (ingress.equals(destNodeConnector)) {
            LOG.info("JBH: In addBidirectionalMacToMacFlows: Source and Destination ports are same. Ingress:{} sourceIP:{} desIP:{} sourceMac:{} desMac:{}",
                    ingress.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue()
                    ,sourceIp.getValue(),desIp.getValue(),souceMac.getValue(),desMac.getValue());
            return;
        }

        // add destMac-To-sourceMac flow on source port
        //addMacIPToMacIPFlow(desMac, souceMac, desIp, sourceIp, ingress);

        // add sourceMac-To-destMac flow on destination port
        //addMacIPToMacIPFlow(souceMac, desMac, sourceIp, desIp, destNodeConnector);
        addMacIPToMacIPFlow(souceMac,desMac,sourceIp,desIp,ingress,destNodeConnector);
        addMacIPToMacIPFlow(desMac,souceMac,desIp,sourceIp,destNodeConnector,ingress);
    }

    /**
     * 下流表：
     * match: ingress、sourceMac、desMac、sourceIP、desIP
     * Action：output to egress
     *
     */
    private void addMacIPToMacIPFlow(MacAddress souceMac, MacAddress desMac, Ipv4Address sourceIp, Ipv4Address desIp , NodeConnectorRef ingress
            ,NodeConnectorRef egress){
        // do not add flow if both macs are same.
        if (souceMac != null && desMac.equals(souceMac)) {
            LOG.info("JBH: In addMacIPToMacIPFlow: No flows added. Source and Destination mac are same.");
            return;
        }
        if(sourceIp != null && sourceIp.equals(desIp)){
            LOG.info("JBH: In addMacIPToMacIPFlow: No flows added. Source and Destination ip are same.");
            return;
        }

        // get flow table key
        InstanceIdentifier<Node> nodeId = egress.getValue().firstIdentifierOf(Node.class);
        if(nodeId==null) {
            LOG.info("JBH: In addMacIPToMacIPFlow: NodeId == null");
            return;
        }
        InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
        InstanceIdentifier<Flow> flowId = getFlowInstanceId(tableId);

        // build a flow that target given mac id
        Flow flowBody = createMacIpToMacIpFlow(flowTableId, flowPriority, souceMac, desMac, sourceIp, desIp, ingress, egress);

        // commit the flow in config data
        writeFlowToConfigData(flowId, flowBody);

    }

    /**
     * create flow：
     * match：ingress、sourceMac、desMac、sourceIP、desIP
     * action：output to egress
     *
     */
    private Flow createMacIpToMacIpFlow(Short tableId, int priority, MacAddress sourceMac
            , MacAddress destMac, Ipv4Address sourceIp, Ipv4Address destIp, NodeConnectorRef ingress, NodeConnectorRef destPort){
        // start building flow
        FlowBuilder MacIpToMacIp = new FlowBuilder() //
                .setTableId(tableId) //
                .setFlowName("MacIp2MacIp");
        // use its own hash code for id.
        MacIpToMacIp.setId(new FlowId(Long.toString(MacIpToMacIp.hashCode())));

        //create a match that has mac ip to mac ip match
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetDestination(new EthernetDestinationBuilder().setAddress(destMac).build())
                .setEthernetSource(new EthernetSourceBuilder().setAddress(sourceMac).build())
                .setEthernetType(new EthernetTypeBuilder()
                        .setType(new EtherType(Long.valueOf(KnownEtherType.Ipv4.getIntValue()))).build());


        Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder()
                .setIpv4Destination(new Ipv4Prefix(destIp.getValue()+"/32"))
                .setIpv4Source(new Ipv4Prefix(sourceIp.getValue()+"/32"));

        Match match = new MatchBuilder()
                .setInPort(ingress.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId()) //add inport to match
                .setEthernetMatch(ethernetMatchBuilder.build())
                .setLayer3Match(ipv4MatchBuilder.build())
                .build();

        Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();

        Action outputAction = new ActionBuilder() //
                .setOrder(0)
                .setAction(new OutputActionCaseBuilder() //
                        .setOutputAction(new OutputActionBuilder() //
                                .setMaxLength(0xffff) //
                                .setOutputNodeConnector(destPortUri) //
                                .build()) //
                        .build()) //
                .build();

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputAction))
                .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()//
                        .setApplyActions(applyActions) //
                        .build()) //
                .build();

        // Put our Instruction in a list of Instructions
        MacIpToMacIp
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

        return MacIpToMacIp.build();

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

    private Future<RpcResult<AddFlowOutput>> writeFlowToConfigData(InstanceIdentifier<Flow> flowPath, Flow flow) {
        final InstanceIdentifier<Table> tableInstanceId = flowPath.<Table>firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeInstanceId = flowPath.<Node>firstIdentifierOf(Node.class);
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowRef(new FlowRef(flowPath));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setTransactionUri(new Uri(flow.getId().getValue()));
        LOG.debug("JBH: In writeFlowToConfigData: match:{} instructions:{}",flow.getMatch().getLayer3Match(),flow.getInstructions());
        return salFlowService.addFlow(builder.build());
    }

    /**
     * 发送packetOut
     * 源mac 目的mac 源ip 目的ip 目的端口
     *
     */
    private void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress){
        if (ingress == null || egress == null) {
            return;
        }

        InstanceIdentifier<Node> egressNodePath = egress.getValue().firstIdentifierOf(Node.class);
        TransmitPacketInputBuilder tb = new TransmitPacketInputBuilder()
                .setEgress(egress)
                .setIngress(ingress)
                .setNode(new NodeRef(egressNodePath))
                .setPayload(payload);

        LOG.debug("JBH: In sendPacketOut: ingress:{} egress:{}",ingress.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId()
                ,egress.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId());
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(packetProcessingService.transmitPacket(tb.build())),
                new FutureCallback<RpcResult<Void>>() {
                    @Override
                    public void onSuccess(RpcResult<Void> result) {
                        LOG.debug("JBH: Send packetOut success");
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        LOG.debug("JBH: transmitPacket for {} failed",ingress);
                    }
                }, MoreExecutors.directExecutor());
    }


    /**
     * More info see l2switch EthernetDecoder
     *
     */
    private int ethernetDecode(byte[] data){
        final Integer ETHERTYPE_8021Q = 0x8100;
        final Integer ETHERTYPE_QINQ = 0x9100;

        try{
            // Deserialize the optional field 802.1Q headers
            Integer nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 96, 16));
            int extraHeaderBits = 0;
            //ArrayList<Header8021q> headerList = new ArrayList<>();
            while (nextField.equals(ETHERTYPE_8021Q) || nextField.equals(ETHERTYPE_QINQ)) {
                Header8021qBuilder headerBuilder = new Header8021qBuilder();
                headerBuilder.setTPID(Header8021qType.forValue(nextField));

                // Read 2 more bytes for priority (3bits), drop eligible (1bit),
                // vlan-id (12bits)
                byte[] vlanBytes = BitBufferHelper.getBits(data, 112 + extraHeaderBits, 16);

                // Remove the sign & right-shift to get the priority code
                headerBuilder.setPriorityCode((short) ((vlanBytes[0] & 0xff) >> 5));

                // Remove the sign & remove priority code bits & right-shift to
                // get drop-eligible bit
                headerBuilder.setDropEligible(1 == (vlanBytes[0] & 0xff & 0x10) >> 4);

                // Remove priority code & drop-eligible bits, to get the VLAN-id
                vlanBytes[0] = (byte) (vlanBytes[0] & 0x0F);
                headerBuilder.setVlan(new VlanId(BitBufferHelper.getInt(vlanBytes)));

                // Add 802.1Q header to the growing collection
                //headerList.add(headerBuilder.build());

                // Reset value of "nextField" to correspond to following 2 bytes
                // for next 802.1Q header or EtherType/Length
                nextField = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 128 + extraHeaderBits, 16));

                // 802.1Q header means payload starts at a later position
                extraHeaderBits += 32;
            }
            // Determine start & end of payload
            int payloadOffset = (112 + extraHeaderBits) / NetUtils.NUM_BITS_IN_A_BYTE;
            return payloadOffset;
        }catch (BufferException e){
            LOG.error("JBH: In onPacketReceived: Exception:{} during decoding raw packet to ethernet.", e);
        }

        return -1;
    }


    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        packetListenerRegistration = notificationService.registerNotificationListener(this);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        packetListenerRegistration.close();
    }

}
