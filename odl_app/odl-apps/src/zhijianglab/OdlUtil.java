package zhijianglab;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;


public class OdlUtil
{
    private String url = null;
    private String rawTopo = null;
    private ArrayList<Switch> switches = new ArrayList<Switch>();
    private ArrayList<Host> hosts = new ArrayList<Host>();
    private String uname = null;
    private String pwd = null;

    public OdlUtil(String host, int port, String uname, String pwd)
    {
        this.url = "http://" + host + ":" + port;
        this.uname = uname;
        this.pwd = pwd;
    }

    public void getTopo()throws IOException
    {
        String result = null;
        HttpRequest hr = new HttpRequest();

        result = hr.doGet(url + "/restconf/operational/network-topology:network-topology/topology/flow:1", uname, pwd);

        rawTopo = result;
    }

    public void parseTopo()throws IOException
    {
        JSONObject jsonObject = JSONObject.fromObject(rawTopo);
        String topology = jsonObject.optString("topology");

        JSONArray jsonArray = JSONArray.fromObject(topology);
        //System.out.println(jsonArray.size());
        jsonObject = jsonArray.getJSONObject(0);

        String node = jsonObject.optString("node");
        String link = jsonObject.optString("link");

        //System.out.println(node);
        //System.out.println(link);

        JSONArray nodeArray = JSONArray.fromObject(node);
        JSONArray linkArray = JSONArray.fromObject(link);


        // parse nodes
        for (int i = 0; i < nodeArray.size(); i++)
        {
            JSONObject nodeObject = nodeArray.getJSONObject(i);

            String nodeType = nodeObject.optString("node-id");

            // find a host
            if (nodeType.charAt(0) == 'h')
            {
                String hostId = nodeType;
                String hostInfo = nodeObject.optString("host-tracker-service:addresses");

                JSONArray hostInfoArray = JSONArray.fromObject(hostInfo);

                JSONObject hostInfoObject = hostInfoArray.getJSONObject(0);

                int nodeId = hostInfoObject.optInt("id");
                String nodeMac = hostInfoObject.optString("mac");
                String nodeIp = hostInfoObject.optString("ip");

                Host newHost = new Host(nodeId, nodeIp, nodeMac, hostId);
                hosts.add(newHost);
                //newHost.showInfo();

            }
            // find a switch
            else if (nodeType.charAt(0) == 'o')
            {
                String switchId = nodeType;

                Switch newSwitch = new Switch(switchId, url, uname, pwd);

                switches.add(newSwitch);
                //newSwitch.showInfo();

            }
            // other devices
            else
            {
                System.out.println("parse error! unknown node type.");
            }
        }
        // parse links
        for (int i = 0; i < linkArray.size(); i++)
        {

        }
    }
    public void showAllHosts()
    {
        for (int i = 0; i < hosts.size(); i++)
        {
            hosts.get(i).showInfo();
        }
    }
    public void showAllSwitchs()
    {
        for (int i = 0; i < switches.size(); i++)
        {
            switches.get(i).showInfo();
        }
    }

    public String setFlowEntry(String nodeId, int tableId, int flowId, Map<String, String> matches, Map<String, String> actions) throws IOException
    {
        String result = null;

        Flowentries fn = new Flowentries();
        HttpRequest hr = new HttpRequest();

        String realUrl = url + "/restconf/config/opendaylight-inventory:nodes/node/" + nodeId + "/flow-node-inventory:table/" + String.valueOf(tableId) + "/flow/" + String.valueOf(flowId);
        String param = fn.genEntryJson(matches, actions);

        result = hr.doPut(realUrl, uname, pwd, param);

        return result;
    }
}
