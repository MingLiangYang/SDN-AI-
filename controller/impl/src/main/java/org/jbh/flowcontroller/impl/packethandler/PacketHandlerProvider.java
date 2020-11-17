package org.jbh.flowcontroller.impl.packethandler;

import org.jbh.flowcontroller.impl.monitor.ControllerMonitor;
import org.jbh.flowcontroller.impl.packethandler.topology.NetworkGraphImpl;
import org.jbh.flowcontroller.impl.packethandler.topology.NetworkGraphService;
import org.jbh.flowcontroller.impl.packethandler.topology.TopologyLinkDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class PacketHandlerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PacketHandlerProvider.class);

    private final DataBroker databroker;
    private final NotificationService notificationService;
    private final SalFlowService salFlowService;
    private final PacketProcessingService packetProcessingService;
    private final ControllerMonitor controllerMonitor;

    private NetworkGraphService graph;
    private TopologyLinkDataChangeListener topologyLinkDataChangeListener;
    private HashMap<String, String> hostToSwitchTP;
    private PacketInHandler packetInHandler;

    private Registration topoLinkListnerReg;

    public PacketHandlerProvider(final DataBroker dataBroker, final NotificationService notificationService
            , SalFlowService salFlowService, PacketProcessingService packetProcessingService
            , ControllerMonitor controllerMonitor){
        this.databroker = dataBroker;
        this.notificationService = notificationService;
        this.salFlowService = salFlowService;
        this.packetProcessingService = packetProcessingService;
        this.controllerMonitor = controllerMonitor;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("JBH: In PacketHandlerProvider: init");
        graph = new NetworkGraphImpl();
        hostToSwitchTP = new HashMap<>();
        topologyLinkDataChangeListener = new TopologyLinkDataChangeListener(databroker, graph, hostToSwitchTP);
        topoLinkListnerReg = topologyLinkDataChangeListener.registerAsDataChangeListener();
        packetInHandler = new PacketInHandler(databroker, notificationService, salFlowService
                , packetProcessingService, controllerMonitor, graph, hostToSwitchTP);
        packetInHandler.registerAsPacketListener();
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("JBH: In PacketHandlerProvider: close");
        if (topoLinkListnerReg != null) {
            topoLinkListnerReg.close();
        }
        if (packetInHandler != null){
            packetInHandler.close();
        }
    }
}
