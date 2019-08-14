package org.jbh.flowcontroller.impl.defender;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PacketHandler implements PacketProcessingListener
{
    //private NotificationPublishService notificationPublishService;
    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    private Map<String,FileWriter> fwMap = new ConcurrentHashMap<>();

    public PacketHandler()
    {
        counter = 0;
        LOG.info("[liuhy] PacketHandler Initiated. ");
    }

    String srcIP, dstIP, ipProtocol, srcMac, dstMac;
    String stringEthType;
    int srcPort, dstPort;
    int counter;
    byte[] payload, srcMacRaw, dstMacRaw, srcIPRaw, dstIPRaw, rawIPProtocol, rawEthType, rawSrcPort, rawDstPort;


    NodeConnectorRef ingressNodeConnectorRef;
    // Ingress Switch Id
    NodeId ingressNodeId;
    // Ingress Switch Port Id from DataStore
    NodeConnectorId ingressNodeConnectorId;
    String ingressConnector, ingressNode;


    @Override
    public void onPacketReceived(PacketReceived notification)
    {
        // TODO Auto-generated method stub

        //LOG.info("[liuhy] enter 1 !!!!!");
        ingressNodeConnectorRef = notification.getIngress();
        ingressNodeConnectorId = InventoryUtility.getNodeConnectorId(ingressNodeConnectorRef);
        ingressConnector = ingressNodeConnectorId.getValue();
        ingressNodeId = InventoryUtility.getNodeId(ingressNodeConnectorRef);
        ingressNode = ingressNodeId.getValue();
        //LOG.info("[liuhy] enter 2 !!!!!");

        //LOG.info("[liuhy] ingressNode " + ingressNode);

        //packetSize = payload.length;
        Date date = new Date();

        payload = notification.getPayload();
        //LOG.info("[liuhy] enter 3 !!!!!");
        int packetSize = payload.length;
        srcMacRaw = PacketParsing.extractSrcMac(payload);
        dstMacRaw = PacketParsing.extractDstMac(payload);
        srcMac = PacketParsing.rawMacToString(srcMacRaw);
        dstMac = PacketParsing.rawMacToString(dstMacRaw);

        rawEthType = PacketParsing.extractEtherType(payload);
        stringEthType = PacketParsing.rawEthTypeToString(rawEthType);

        if (dstMac.equals("FF:FF:FF:FF:FF:FF") && stringEthType.equals("806"))
        {
            LOG.info("[liuhy] This is an ARP packet ");
            LOG.info("[liuhy] Received packet from MAC {} to MAC {}, EtherType=0x{} ", srcMac, dstMac, stringEthType);
        }
        else if (stringEthType.equals("800"))
        {
            dstIPRaw = PacketParsing.extractDstIP(payload);
            srcIPRaw = PacketParsing.extractSrcIP(payload);
            dstIP = PacketParsing.rawIPToString(dstIPRaw);
            srcIP = PacketParsing.rawIPToString(srcIPRaw);

            rawIPProtocol = PacketParsing.extractIPProtocol(payload);
            ipProtocol = PacketParsing.rawIPProtoToString(rawIPProtocol).toString();

            rawSrcPort = PacketParsing.extractSrcPort(payload);
            srcPort = PacketParsing.rawPortToInteger(rawSrcPort);
            rawDstPort = PacketParsing.extractDstPort(payload);
            dstPort = PacketParsing.rawPortToInteger(rawDstPort);

            if (dstIP.substring(0, 3).equals("224") || dstIP.equals("255.255.255.255")
                    || srcIP.equals("0.0.0.0") || dstIP.substring(0, 3).equals("225")
                    || dstIP.substring(dstIP.length() -3,dstIP.length()).equals("255")){
                return;
            }

            //use stringbuilder replay string
            StringBuilder content_sb = new StringBuilder();
            //String content = "Time " + time + " src_IP " + srcIP + " dst_IP " + dstIP + " EtherType 0x0" + stringEthType
              //      + " srcProt " + srcPort + " dstPort " + dstPort + " size " + String.valueOf(packetSize);
            content_sb
                    .append("Time ").append(date.getTime())
                    .append(" src_IP ").append(srcIP)
                    .append(" dst_IP ").append(dstIP)
                    .append(" EtherType 0x0").append(stringEthType)
                    .append(" srcProt ").append(srcPort)
                    .append(" dstPort ").append(dstPort)
                    .append(" size ").append(String.valueOf(packetSize));


            if(!fwMap.containsKey(ingressNode)){
                LOG.debug("JBH: In defender: new FileWriter");
                String path = "/home/zju/" + ingressNode + "_pktin.txt";
                //String path = "D:/" + ingressNode + "_pktin.txt";
                fwMap.putIfAbsent(ingressNode,new FileWriter(path));
            }
            fwMap.get(ingressNode).writeLine(content_sb.toString());

            //LOG.debug("[liuhy] Received packet from IP {} to IP {}, EtherType=0x{} ", srcIP, dstIP, stringEthType);

        }

        counter = counter + 1;
        LOG.debug("[liuhy] Totally receive {} packets for now ", counter);
    }

    public void closeFileWriter(){
        for(FileWriter fw : fwMap.values()){
            fw.close();
        }
    }


}