package org.jbh.flowcontroller.impl.ovsstats;


import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.jbh.flowcontroller.impl.defender.FileWriter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableStatisticsGatheringStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.snapshot.gathering.status.grouping.SnapshotGatheringStatusEnd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class statsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(statsProvider.class);

    private DataBroker dataBroker;


    private static final long STATS_DELAY_MILL = 10*1000; // 延迟20s启动统计
    private static final long STATS_INTERVAL_MILL = 3*1000; // 周期3s
    private static final String DATE_AND_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private Map<String,DateAndTime> nodeTimeMap = new HashMap<>();

    private static String PORT_STATS_PATH;
    private static String FLOW_STATS_PATH;
    private FileWriter portfw;
    private FileWriter flowfw;

    public statsProvider(DataBroker dataBroker){
        this.dataBroker = dataBroker;

        //different OS has different path name
        String osName = System.getProperty("os.name"); //操作系统名称
        if(osName!=null && !osName.isEmpty() && osName.substring(0,1).equals("W")){
            PORT_STATS_PATH = "D:/port_stats.txt";
            FLOW_STATS_PATH = "D:/flow_stats.txt";
        }else{
            PORT_STATS_PATH = "/home/zju/port_stats.txt";
            FLOW_STATS_PATH = "/home/zju/flow_stats.txt";
        }

        portfw = new FileWriter(PORT_STATS_PATH);
        flowfw = new FileWriter(FLOW_STATS_PATH);
    }

    /**
     * 周期读取DS的统计信息
     * STATS_INTERVAL_MILL = 3*1000; // 周期3s
     * 写入offline文件
     */
    private class ScheduleStatsTask extends TimerTask {

        public void run() {
            //读取DS中的Nodes
            //遍历每个Nodes.Node
            //获取Node.snapshot-gathering-status-end的时间和状态
            //时间如果更新了 而且end状态存在且为success
            //读取统计信息 并且写入offline文件
            //时间：
            //port统计信息
            //flow统计信息

            //需要的数据结构：MAP{Node 时间}
            try{
                LOG.debug("JBH: In ScheduleStatsTask.run: read Nodes");
                final InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesIdBuilder
                        = InstanceIdentifier.builder(Nodes.class);
                Nodes nodesData = read(LogicalDatastoreType.OPERATIONAL,nodesIdBuilder.build());

                List<Node> listNode = nodesData.getNode();
                if(listNode == null || listNode.isEmpty()){
                    LOG.debug("JBH: In ScheduleStatsTask.run: no node information");
                    return;
                }
                for(Node node : listNode){
                    String datapath = node.getId().getValue();
                    if(isUpdate(datapath,node)){
                        DateAndTime dateAndTime = node
                                .getAugmentation(FlowCapableStatisticsGatheringStatus.class)
                                .getSnapshotGatheringStatusEnd()
                                .getEnd();
                        LOG.debug("JBH: In isUpdate: status end is updated, new dateAndTime:{}",dateAndTime.getValue());

                        writeOneNodeStatsData(datapath,node,dateAndTime);
                    }
                }
            }catch(Exception e){
                LOG.error("JBH: In ScheduleStatsTask.run: Error:{}",e);
            }

        }

    }


    /**
     *
     * write port stats And *table0*'s flow stats to offline files
     *
     */
    private void writeOneNodeStatsData(String datapath, Node node, DateAndTime dateAndTime){

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_AND_TIME_FORMAT);
        String date = dateAndTime.getValue();
        Date date2 = null;
        try{
            date2 = simpleDateFormat.parse(date);
        }catch(ParseException e){
            LOG.error("JBH: In simpleDateFormat.parse(date): ParseException:{}",e);
        }
        if(date2==null){
            return;
        }
        long time = date2.getTime();

        StringBuilder port_sb = new StringBuilder();
        StringBuilder flow_sb = new StringBuilder();

        List<NodeConnector> listnc = node.getNodeConnector();
        if(listnc == null){
            LOG.debug("JBH: In ScheduleStatsTask.run: no nodeconnector information");
            return;
        }
        String ncName;
        FlowCapableNodeConnectorStatistics fcncStats;
        BigInteger packetsReceived;
        BigInteger packetsTransmitted;
        BigInteger bytesReceived;
        BigInteger bytesTransmitted;
        BigInteger packetsReceiveDrops;
        BigInteger packetsTransmitDrops;
        for(NodeConnector nc : listnc){
            ncName = nc.getId().getValue();
            int length = ncName.length();
            if(ncName.substring(length-5,length).equals("LOCAL")){
                continue;
            }
            if(nc.getAugmentation(FlowCapableNodeConnectorStatisticsData.class)==null){
                LOG.debug("JBH: In ScheduleStatsTask.run: no FlowCapableNodeConnectorStatisticsData information");
                continue;
            }
            fcncStats = nc.getAugmentation(FlowCapableNodeConnectorStatisticsData.class).getFlowCapableNodeConnectorStatistics();
            if(fcncStats == null){
                LOG.debug("JBH: In ScheduleStatsTask.run: no FlowCapableNodeConnectorStatistics information");
                continue;
            }
            packetsReceived = fcncStats.getPackets().getReceived();
            packetsTransmitted = fcncStats.getPackets().getTransmitted();
            bytesReceived = fcncStats.getBytes().getReceived();
            bytesTransmitted = fcncStats.getBytes().getTransmitted();
            packetsReceiveDrops = fcncStats.getReceiveDrops();
            packetsTransmitDrops = fcncStats.getTransmitDrops();



            port_sb
                    .append("Time ").append(time)
                    .append(" Switch ").append(datapath)
                    .append(" Port ").append(ncName)
                    .append(" PacketReceivedAll ").append(packetsReceived.toString())
                    .append(" PacketReceivedSuccess ").append(packetsReceived.subtract(packetsReceiveDrops).toString())
                    .append(" PacketsTransmittedAll ").append(packetsTransmitted.toString())
                    .append(" PacketTransmittedSuccess ").append(packetsTransmitted.subtract(packetsTransmitDrops).toString())
                    .append(" ByteReceivedAll ").append(bytesReceived.toString())
                    .append(" ByteTransmittedAll ").append(bytesTransmitted.toString());

            LOG.debug("JBH: In for port: writeOfflineFile: {}",port_sb.toString());
            portfw.writeLine(port_sb.toString());

            port_sb.delete(0,port_sb.length()); //清空一下stringBuffer
        }

        List<Table> listTable = node.getAugmentation(FlowCapableNode.class).getTable();
        if(listTable == null){
            LOG.debug("JBH: In ScheduleStatsTask.run: no table information");
            return;
        }
        List<Flow> listFlow;
        String flowId;
        int flowPriority;
        Counter64 packetCount;
        Counter64 byteCount;
        NodeConnectorId inport;
        Uri outportUri;
        Ipv4Match ipv4Match;
        String sourceIpPrefix;
        String destIpPrefix;

        for(Table table : listTable){
            //only for table 0
            if(table.getId()==0){

                listFlow = table.getFlow();
                if(listFlow == null){
                    LOG.debug("JBH: In ScheduleStatsTask.run: no flow information");
                    return;
                }
                for(Flow flow : listFlow){

                    flowId = flow.getId().getValue();
                    flowPriority = flow.getPriority();
                    LOG.debug("JBH: In for flow: flowId:{} flowPriority:{}",flowId,flowPriority);
                    //only count macIpToMacIpFlow
                    if(flowId.substring(0,9).equals("flowTest-") && flowPriority == 10){
                        //only need flow which has ipv4 match
                        if(flow.getMatch().getLayer3Match() instanceof Ipv4Match){
                            ipv4Match = (Ipv4Match) flow.getMatch().getLayer3Match();
                            LOG.debug("JBH: In for flow: get Ipv4Match");
                        }else{
                            LOG.debug("JBH: In for flow: don't get Ipv4Match");
                            continue ;
                        }

                        sourceIpPrefix = ipv4Match.getIpv4Source().getValue();
                        destIpPrefix = ipv4Match.getIpv4Destination().getValue();
                        if(flow.getAugmentation(FlowStatisticsData.class) == null
                                || flow.getAugmentation(FlowStatisticsData.class).getFlowStatistics() == null){
                            LOG.debug("JBH: In ScheduleStatsTask.run: no FlowStatisticsData information");
                            continue;
                        }
                        packetCount = flow.getAugmentation(FlowStatisticsData.class).getFlowStatistics().getPacketCount();
                        byteCount = flow.getAugmentation(FlowStatisticsData.class).getFlowStatistics().getByteCount();
                        inport = flow.getMatch().getInPort();
                        outportUri = getOutputUriFromFlow(flow);

                        flow_sb
                                .append("Time ").append(time)
                                .append(" Switch ").append(datapath)
                                .append(" SourceIp ").append(sourceIpPrefix.substring(0,sourceIpPrefix.length()-3))
                                .append(" DestIp ").append(destIpPrefix.substring(0,destIpPrefix.length()-3))
                                .append(" InPort ").append(( inport == null ? "xxx" : inport.getValue() ))
                                .append(" OutPort ").append(( outportUri == null ? "xxx" : outportUri.getValue() ))
                                .append(" PacketCount ").append(packetCount.getValue().toString())
                                .append(" ByteCount ").append(byteCount.getValue().toString());

                        LOG.debug("JBH: In for flow: writeOfflineFile{}",flow_sb.toString());
                        flowfw.writeLine(flow_sb.toString());

                        flow_sb.delete(0,flow_sb.length());
                    }

                }

            }

        }

    }


    /**
     *
     * @return output URI from flow if presence otherwise return null
     */
    private Uri getOutputUriFromFlow(Flow flow){
        List<Instruction> instructionList = flow.getInstructions().getInstruction();
        for(Instruction instruction : instructionList){

            //only need order:0 instruction and getInstruction is applyActionsCase
            if(instruction.getOrder() == 0){

                if(instruction.getInstruction() instanceof ApplyActionsCase){
                    ApplyActionsCase applyActionsCase = (ApplyActionsCase) instruction.getInstruction();
                    List<Action> actionList = applyActionsCase.getApplyActions().getAction();
                    //only need order:0 action
                    for(Action action : actionList){
                        if(action.getOrder()==0 && action.getAction() instanceof OutputActionCase){
                            OutputActionCase outputActionCase = (OutputActionCase) action.getAction();
                            return outputActionCase.getOutputAction().getOutputNodeConnector();
                        }
                    }
                }else{ break; }

            }

        }
        return null;
    }


    /**
     * 判断: 首先end的状态必须存在, 而且end状态为success, end的时间存在
     *      新的end比记录的时间新
     *      或者原来的map里没有end
     *
     * @return true if status end is update
     *         false if status is null or status is not updated
     */
    private boolean isUpdate(String datapath, Node node){
        SnapshotGatheringStatusEnd snapshotGatheringStatusEnd = node
                .getAugmentation(FlowCapableStatisticsGatheringStatus.class)
                .getSnapshotGatheringStatusEnd();
        if(snapshotGatheringStatusEnd == null){
            LOG.debug("JBH: In isUpdate: snapshotGatheringStatusEnd is null, stop writeFile");
            return false;
        }

        boolean isSucceeded = snapshotGatheringStatusEnd.isSucceeded();
        DateAndTime dateAndTime = snapshotGatheringStatusEnd.getEnd();
        if(!isSucceeded || dateAndTime == null){
            LOG.debug("JBH: In isUpdate: isSucceeded is {}, dateAndTime is {}, stop writeFile",isSucceeded,dateAndTime);
            return false;
        }

        if(!nodeTimeMap.containsKey(datapath)){
            nodeTimeMap.putIfAbsent(datapath,dateAndTime);
            return true;
        }else {
            if(!nodeTimeMap.get(datapath).getValue().equals(dateAndTime.getValue())) {
                nodeTimeMap.putIfAbsent(datapath,dateAndTime);
                return true;
            }
        }
        return false;
    }

    /** general read function
     *  result is not null if reading succeed,
     *  else result is null.
     */
    private <D extends DataObject> D read(final LogicalDatastoreType store, final InstanceIdentifier<D> path) {
        D result = null;
        final ReadOnlyTransaction readtx = dataBroker.newReadOnlyTransaction();
        Optional<D> optionalDataObject;
        final CheckedFuture<Optional<D>, ReadFailedException> future = readtx.read(store, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("JBH: In read: {}: Failed to read:{}", Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (final ReadFailedException e) {
            LOG.warn("JBH: In read: Failed to read:{}, error:{}", path, e);
        }
        readtx.close();
        return result;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        Timer timer;
        timer = new Timer(); // JBH:当Timer的构造器被调用时，它创建了一个线程，这个线程可以用来调度任务
        timer.schedule(new ScheduleStatsTask(),STATS_DELAY_MILL, STATS_INTERVAL_MILL);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        portfw.close();
        flowfw.close();
    }

}
