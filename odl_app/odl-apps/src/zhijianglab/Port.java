package zhijianglab;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.annotation.processing.SupportedSourceVersion;


public class Port
{
    private String portId = null;
    private int recevBytes = 0;
    private int transBytes = 0;
    private int recevPkts = 0;
    private int transPkts = 0;
    private String nickName = null;
    private int portNumber = 0;
    private String portMac = null;

    public Port(JSONObject portObject)
    {
        this.portId = portObject.optString("id");
        //System.out.println(this.portId.substring(this.portId.length() - 5, this.portId.length()));
        String flowInfo = portObject.optString("opendaylight-port-statistics:flow-capable-node-connector-statistics");
        JSONObject flowInfoObject = JSONObject.fromObject(flowInfo);
        String byteInfo = flowInfoObject.optString("bytes");
        JSONObject byteInfoObject = JSONObject.fromObject(byteInfo);
        this.recevBytes = byteInfoObject.optInt("received");
        this.transBytes = byteInfoObject.optInt("transmitted");

        String pktInfo = flowInfoObject.optString("packets");
        JSONObject pktInfoObject = JSONObject.fromObject(pktInfo);
        this.recevPkts = pktInfoObject.optInt("received");
        this.transPkts = pktInfoObject.optInt("transmitted");

        this.nickName = portObject.optString("flow-node-inventory:name");
        this.portNumber = portObject.optInt("flow-node-inventory:port-number");
        this.portMac = portObject.optString("flow-node-inventory:hardware-address");


    }
    public void showInfo(String path)
    {
        FileWriter fw = new FileWriter(path);

        fw.writeLine("portId "+ this.portId);
        fw.writeLine("recevBytes " + this.recevBytes);
        fw.writeLine("transBytes " + this.transBytes);
        fw.writeLine("recevPkts " + this.recevPkts);
        fw.writeLine("transPkts " + this.transPkts);
        fw.writeLine("portNumber " + this.portNumber);
        fw.writeLine("portMac " + this.portMac);


        //System.out.println("########## " + this.nickName + " info ##########");
        //System.out.println("portId : " + this.portId);
        //System.out.println("recevBytes : " + this.recevBytes);
        //System.out.println("transBytes : " + this.transBytes);
        //System.out.println("recevPkts : " + this.recevPkts);
        //System.out.println("transPkts : " + this.transPkts);
        //System.out.println("nickName" + this.nickName);
        //System.out.println("portNumber : " + this.portNumber);
        //System.out.println("portMac : " + this.portMac);
        //System.out.println("########## "+ this.nickName +" info end ##########");
        //System.out.println();
    }
}
