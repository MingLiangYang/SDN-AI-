package org.jbh.flowcontroller.impl.monitor;

import org.hyperic.sigar.*;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**控制器监控类  主要提供以下功能：
 *
 * 每秒的  PacketIn总速率 、每个交换机的PacketIn速率 、 主机CPU 、 控制器JVM 、 主机IO
 * 监听  交换机节点的上线和下线   主机的上线和下线   地址记录的增加/减少情况   链路的链接和断开情况
 *
 * 写到额外的LOG文件里
 */
public class ControllerMonitor implements PacketProcessingListener, DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerMonitor.class);

    private DataBroker dataBroker;
    private NotificationService notificationService;
    private ListenerRegistration nodeRegistration;
    private ListenerRegistration packetListenerRegistration;

    private static final long MONITOR_DELAY_MILL = 1*1000;   // 延迟1s启动控制器监控
    private static final long MONITOR_INTERVAL_MILL = 1*1000; // 周期1s
    static final byte[] ETH_TYPE_IPV4 = new byte[] { 0x08, 0x00 };
    private final Sigar sigar = new Sigar();
    private final Runtime r = Runtime.getRuntime();
    private final Properties props = System.getProperties();
    private final DecimalFormat df = new DecimalFormat("#.####");
    private String controllerIP = "192.168.33.219";

    private Map<String,Long> switchPacketInNumOneSec = new ConcurrentHashMap<>();  //交换机PacketIn速率
    private AtomicLong packetInNumOneSec = new AtomicLong(0);           //PacketIn速率
    private Long pacRev = 0L;   //记录上一秒的控制器网卡的收发包总数目
    private Long pacSend = 0L;
    private Long byteRev = 0L;
    private Long byteSend = 0L;
    private Long dropRev = 0L;
    private Long dropSend = 0L;

    public ControllerMonitor(DataBroker dataBroker,NotificationService notificationService){
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change: changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            String before,after;
            switch (rootNode.getModificationType()) {
                case WRITE:
                    before = (rootNode.getDataBefore() == null) ? "null" : rootNode.getDataBefore().getId().getValue();
                    after = (rootNode.getDataAfter() == null) ? "null" : rootNode.getDataAfter().getId().getValue();
                    LOG.info("JBH: In Monitor: In onDataTreeChange: Write a Switch to DS " +
                            "switchBefore:{} switchAfter:{}",before,after);
                    break;
                case DELETE:
                    before = (rootNode.getDataBefore() == null) ? "null" : rootNode.getDataBefore().getId().getValue();
                    after = (rootNode.getDataAfter() == null) ? "null" : rootNode.getDataAfter().getId().getValue();
                    LOG.info("JBH: In Monitor: In onDataTreeChange: DELETE a Switch from DS " +
                            "switchBefore:{} switchAfter:{}",before,after);
                    break;
                default:
                    break;
            }
        }
    }


    /**监控 PacketInSpeed 和 每个交换机的 PacketInSpeed  收到一个包就+1
     *
     */
    @Override
    public void onPacketReceived(PacketReceived notification) {
        try{
            byte[] etherType = Arrays.copyOfRange(notification.getPayload(), 12, 14);
            // IPv4 traffic only
            if (Arrays.equals(ETH_TYPE_IPV4, etherType)) {
                packetInNumOneSec.incrementAndGet();

                String datapath = notification.getIngress().getValue().firstIdentifierOf(Node.class)    //"openflow:123"
                        .firstKeyOf(Node.class, NodeKey.class).getId().getValue();
                if(datapath!=null){
                    if(!switchPacketInNumOneSec.containsKey(datapath)){
                        switchPacketInNumOneSec.putIfAbsent(datapath,1L);
                    } else { switchPacketInNumOneSec.put(datapath,switchPacketInNumOneSec.get(datapath)+1); }
                }
            }
        }catch (Exception e){
            LOG.debug("JBH: In Monitor: In onPacketReceived: get a exception:{}",e);
        }
    }

    /**
     * cpu monitor
     *
     * @param sb
     */
    private void cpuMonitor(StringBuilder sb) throws SigarException{
        if(sb==null) return;

        CpuPerc cpuList[] = sigar.getCpuPercList();
        double cpuUsageSum = 0;
        for (int i = 0; i < cpuList.length; i++) {
            cpuUsageSum += cpuList[i].getCombined();
            //sb.append(" Cpu").append(i).append(":").append(cpuList[i].getCombined());
        }
        sb.append(" CpuAve:").append(df.format(cpuUsageSum / cpuList.length));
    }

    /**
     * memory monitor
     */
    private void memoryMonitor(StringBuilder sb){
        if(sb==null) return;
        sb.append(" JVMUsedMemory(MB):").append((r.totalMemory()-r.freeMemory())/1024/1024);
    }

    /**
     * Controller NIO monitor
     * 如果是控制器的IP地址，输出该网卡过去1s内的收发包速率
     * 不输出127.0.0.1和其他IP网卡的收发包信息
     *
     */
    private void IOMonitor(StringBuilder sb) throws  SigarException{
        if(sb==null) return;

        String ifNames[] = sigar.getNetInterfaceList();
        for (int i = 0; i < ifNames.length; i++) {
            String name = ifNames[i];
            NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);

            String IPName = ifconfig.getAddress();

            if (!IPName.equals(controllerIP)) continue;
            if ((ifconfig.getFlags() & 1L) <= 0L) continue;


            NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
            sb
                    .append(" ").append(IPName)
                    .append("-pacRev:").append(ifstat.getRxPackets()-pacRev)
                    .append("-pacSend:").append(ifstat.getTxPackets()-pacSend)
                    .append("-byteRev:").append(ifstat.getRxBytes()-byteRev)
                    .append("-byteSend:").append(ifstat.getTxBytes()-byteSend)
                    .append("-dropPacRev:").append(ifstat.getRxDropped()-dropRev)
                    .append("-dropPacSend:").append(ifstat.getTxDropped()-dropSend);
            pacRev = ifstat.getRxPackets();
            pacSend = ifstat.getTxPackets();
            byteRev = ifstat.getRxBytes();
            byteSend = ifstat.getTxBytes();
            dropRev = ifstat.getRxDropped();
            dropSend = ifstat.getTxDropped();
            return;
        }
    }

    private class ScheduleMonitorTask extends TimerTask {
        public void run() {
            StringBuilder CPUMonitorContent = new StringBuilder();
            StringBuilder monitorContent = new StringBuilder();

            try{
                //资源采样：HostCPU JVMMemory IO
                cpuMonitor(CPUMonitorContent);
                memoryMonitor(CPUMonitorContent);
                IOMonitor(CPUMonitorContent);
            } catch(SigarException e){
                LOG.error("JBH: In Monitor: In TimerTask: Get a exception:{}" ,e);
            }

            try{
                //监控packetIn信息
                monitorContent
                        .append("JBH: In Monitor:")
                        .append(" PacketInSpeed:").append(packetInNumOneSec.get());

                for(Map.Entry<String, Long> entry : switchPacketInNumOneSec.entrySet()){
                    monitorContent
                            .append(" ").append(entry.getKey()).append("SPEED:").append(entry.getValue());
                }

                monitorContent.append(CPUMonitorContent);

                //打印控制器监控信息
                LOG.debug(monitorContent.toString());

                //清空下一秒监控信息
                switchPacketInNumOneSec.clear();
                packetInNumOneSec.set(0);
            }catch(Exception e){
                LOG.error("JBH: In Monitor: In timer: error caught:{}",e);
            }
        }
    }

    /**
     * 输出主机和JVM的总信息
     */
    private void systemInfo() throws UnknownHostException,SigarException {
        Mem mem = sigar.getMem();
        Swap swap = sigar.getSwap();
        InetAddress addr;
        addr = InetAddress.getLocalHost();
        String ip = addr.getHostAddress();
        Map<String, String> map = System.getenv();
        String userName = map.get("USERNAME");// 获取用户名
        String computerName = map.get("COMPUTERNAME");// 获取计算机名
        String userDomain = map.get("USERDOMAIN");// 获取计算机域名
        LOG.info("JBH: systemInfo: username:{}",userName);
        LOG.info("JBH: systemInfo: computerName:{}",computerName);
        LOG.info("JBH: systemInfo: userDomain:{}",userDomain);
        LOG.info("JBH: systemInfo: ip:{}",ip);
        LOG.info("JBH: systemInfo: hostname:{}",addr.getHostName());
        LOG.info("JBH: systemInfo: totalMemory(MB):{}",mem.getTotal()/1024/1024);
        LOG.info("JBH: systemInfo: usedMemory(MB):{}",mem.getUsed()/1024/1024);
        LOG.info("JBH: systemInfo: freeMemory(MB):{}",mem.getFree()/1024/1024);
        LOG.info("JBH: systemInfo: swap total(MB):{}",swap.getTotal()/1024/1024);
        LOG.info("JBH: systemInfo: swap used(MB):{}",swap.getUsed()/1024/1024);
        LOG.info("JBH: systemInfo: swap free(MB):{}",swap.getFree()/1024/1024);
        LOG.info("JBH: systemInfo: java JVMMaxMemory(MB):{}",r.maxMemory()/1024/1024);
        LOG.info("JBH: systemInfo: java availableProcessors:{}",r.availableProcessors());
        LOG.info("JBH: systemInfo: java version:{}",props.getProperty("java.version"));
        LOG.info("JBH: systemInfo: java home:{}" ,props.getProperty("java.home"));
        LOG.info("JBH: systemInfo: java classpath:{}",props.getProperty("java.class.path"));
        LOG.info("JBH: systemInfo: java libraryPath:{}",props.getProperty("java.library.path"));
        LOG.info("JBH: systemInfo: java io.tmpdir:{}",props.getProperty("java.io.tmpdir"));
        LOG.info("JBH: systemInfo: java ext.dirs:{}",props.getProperty("java.ext.dirs"));
        LOG.info("JBH: systemInfo: osName:{}",props.getProperty("os.name"));
        LOG.info("JBH: systemInfo: osArch:{}",props.getProperty("os.arch"));
        LOG.info("JBH: systemInfo: osVersion:{}",props.getProperty("os.version"));
        LOG.info("JBH: systemInfo: file separator:{}",props.getProperty("file.separator"));
        LOG.info("JBH: systemInfo: path separator:{}",props.getProperty("path.separator"));
        LOG.info("JBH: systemInfo: line separator:{}",props.getProperty("line.separator"));
        LOG.info("JBH: systemInfo: user name:{}",props.getProperty("user.name"));
        LOG.info("JBH: systemInfo: user home:{}",props.getProperty("user.home"));
        LOG.info("JBH: systemInfo: user dir:{}",props.getProperty("user.dir"));
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        try{
            systemInfo();
        }catch(Exception e){
            LOG.error("JBH: systemInfo: get error:{}",e);
        }


        //注册DataTreeChangeListener
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();
        DataTreeIdentifier<Node> treeIdentifier = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier);
        nodeRegistration = dataBroker.registerDataTreeChangeListener(treeIdentifier, this);

        //注册packetIn notification listener
        packetListenerRegistration = notificationService.registerNotificationListener(this);

        //创建定时任务
        Timer timer;
        timer = new Timer(); // JBH:当Timer的构造器被调用时，它创建了一个线程，这个线程可以用来调度任务
        timer.schedule(new ScheduleMonitorTask(),MONITOR_DELAY_MILL, MONITOR_INTERVAL_MILL);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        nodeRegistration.close();
        packetListenerRegistration.close();
    }



}
