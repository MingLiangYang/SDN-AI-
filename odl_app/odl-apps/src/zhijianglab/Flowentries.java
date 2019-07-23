package zhijianglab;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import zhijianglab.Action;
import zhijianglab.Match;

import javax.swing.plaf.synth.SynthLookAndFeel;
import java.util.ArrayList;
import java.util.Map;


public class Flowentries
{
    private String entryId = null;
    private ArrayList<Action> actions = new ArrayList<Action>();
    private int byteCnt = 0;
    private int pktCnt = 0;
    private int priority = 0;
    private Match matchField = null;

    public Flowentries()
    {

    }
    public Flowentries(JSONObject flowEntryObject)
    {
        this.entryId = flowEntryObject.optString("id");


        String staticInfo = flowEntryObject.optString("opendaylight-flow-statistics:flow-statistics");
        JSONObject staticJsonObject = JSONObject.fromObject(staticInfo);
        this.byteCnt = staticJsonObject.optInt("byte-count");
        this.pktCnt = staticJsonObject.optInt("packet-count");
        this.priority = flowEntryObject.optInt("priority");

        /*
        actions
         */

        String instructionInfo = flowEntryObject.optString("instructions");
        if (instructionInfo.isEmpty())
        {
            //System.out.println("this entry no actions");
        }
        else
        {
            JSONObject instructionObj = JSONObject.fromObject(instructionInfo);
            String instructionArray = instructionObj.optString("instruction");
            JSONArray insArray = JSONArray.fromObject(instructionArray);

            //maybe many instructions
            JSONObject insObj = insArray.getJSONObject(0);
            //System.out.println(insObj.toString());
            String aplActionArrayInfo = insObj.optString("apply-actions");

            JSONObject aplActionObject = JSONObject.fromObject(aplActionArrayInfo);

            String actionArrayInfo = aplActionObject.optString("action");

            JSONArray actionArray = JSONArray.fromObject(actionArrayInfo);

            for (int i = 0; i < actionArray.size(); i++)
            {
                JSONObject perActionObject = actionArray.getJSONObject(i);

                Action newAction = new Action(perActionObject);

                actions.add(newAction);

                //newAction.showInfo();

            }
        }

        //System.out.println(actions.size());


        /*
        match
         */
        String matchInfo = flowEntryObject.getString("match");


        //System.out.println("11111111111111111111111");

        // no match fields
        if (matchInfo.equals("{}"))
        {
            matchField = null;
            //System.out.println("11111111111111111111111");
        }
        else
        {
            JSONObject matchObject = JSONObject.fromObject(matchInfo);
            //System.out.println(matchObject.toString());
            matchField = new Match(matchObject);
            //matchField.showInfo();
        }

    }
    public void showInfo(String path)
    {

        FileWriter fw = new FileWriter(path);

        fw.writeLine("byteCnt " + this.byteCnt);
        fw.writeLine("pktCnt " + this.pktCnt);
        fw.writeLine("priority " + this.priority);

        //System.out.println("########## entry ： " + this.entryId + " info ##########");
        //System.out.println("entryId : " + this.entryId);
        //System.out.println("byteCnt : " + this.byteCnt);
        //System.out.println("pktCnt : " + this.pktCnt);
        //System.out.println("priority : " + this.priority);

        if (matchField != null)
        {
            matchField.showInfo();
        }
        else
        {
            System.out.println("there is no match field in this entry");
        }




        //System.out.println("########## entry ：" + this.entryId + " info end ##########");
        //System.out.println();


    }


    /*
    {
        "flow": [
            {
                "id": "555",
                "match": {
                    "ethernet-match": {
                        "ethernet-source": {
                            "address": "aaaaa"
                        },
                        "ethernet-destination": {
                            "address": "bbbbb"
                        },
                        "ethernet-type": {
                            "type": "0x0800"
                        }
                    },
                    "ipv4-source": "10.0.0.1/32",
                    "ipv4-destination": "10.0.0.2/32"
                },
                "instructions": {
                    "instruction": [
                        {
                            "order": "0",
                            "apply-actions": {
                                "action": [
                                    {
                                        "order": "0",
                                        "drop-action": {}
                                    }
                                ]
                            }
                        }
                    ]
                },
                "priority": "2000",
                "table_id": "0"
            }
        ]
    }
     */


    public String genEntryJson(Map<String, String> matches, Map<String, String> actions)
    {
        String result = "";

        JSONObject matchJson = new JSONObject();
        String matchField = "";

        JSONObject srcMacJson = new JSONObject();
        String srcMac = matches.get("src_mac");
        srcMacJson.put("address", srcMac);
        srcMac = srcMacJson.toString();
        srcMacJson.clear();
        matchJson.put("ethernet-source", srcMac);
        //srcMac = srcMacJson.toString();

        JSONObject dstMacJson = new JSONObject();
        String dstMac = matches.get("dst_mac");
        dstMacJson.put("address", dstMac);
        dstMac = dstMacJson.toString();
        dstMacJson.clear();
        matchJson.put("ethernet-destination", dstMac);
        //dstMac = dstMacJson.toString();

        JSONObject ethTypeJson = new JSONObject();
        String ethType = matches.get("eth-type");
        ethTypeJson.put("type", ethType);
        ethType = ethTypeJson.toString();
        ethTypeJson.clear();
        matchJson.put("ethernet-type", ethType);


        String ethField = matchJson.toString();
        matchJson.clear();
        matchJson.put("ethernet-match", ethField);

        //System.out.println(matchJson.toString());

        String srcIpField = matches.get("src_ip");
        matchJson.put("ipv4-source", srcIpField);

        String dstIpField = matches.get("dst_ip");
        matchJson.put("ipv4-destination", dstIpField);

        //System.out.println(matchJson.toString());

        matchField = matchJson.toString();

        JSONArray actionJsonArray = new JSONArray();

        int actionOrder = 0;
        for (Map.Entry<String, String> entry : actions.entrySet())
        {
            String actionTypt = entry.getKey();
            String actionContent = entry.getValue();
            if (actionTypt.charAt(0) == 'o')
            {
                /*
                {
                    "order": 1,
                    "output-action": {
                        "output-node-connector": "3",
                        "max-length": 0
                    }
                }
                 */

                JSONObject outputJson = new JSONObject();
                outputJson.put("output-node-connector", actionContent);
                outputJson.put("max-length", 0);
                String outPutField = outputJson.toString();
                outputJson.clear();
                outputJson.put("order", actionOrder);
                outputJson.put("output-action", outPutField);

                actionJsonArray.add(outputJson);



            }
            else if (actionTypt.charAt(0) == 'd')
            {
                /*
                {
                    "order": "0",
                    "drop-action": {}
                }
                 */
                JSONObject dropJson = new JSONObject();
                dropJson.put("order", String.valueOf(actionOrder));
                dropJson.put("drop-action", "{}");

                actionJsonArray.add(dropJson);

            }
            else if (actionTypt.charAt(0) == 'c')
            {

                /*
                {
                    "order": 2,
                    "output-action": {
                        "output-node-connector": "CONTROLLER",
                        "max-length": 65535
                    }
                }
                 */

                JSONObject pktInJson = new JSONObject();
                pktInJson.put("output-node-connector", "CONTROLLER");
                pktInJson.put("max-length", 65535);
                String pktInField = pktInJson.toString();
                pktInJson.clear();
                pktInJson.put("order", actionOrder);
                pktInJson.put("output-action", pktInField);

                actionJsonArray.add(pktInJson);
            }
            actionOrder ++;
        }

        String insinstructionsField = "{\"instruction\":[{\"order\":0,\"apply-actions\":{\"action\":" + actionJsonArray.toString() + "}}]}";


        String idField = matches.get("id");
        String outPortField = matches.get("out_port");
        String flowNameField = matches.get("flow-name");
        String priorityField = matches.get("priority");
        String timeOutField = matches.get("timeout");

        JSONObject fullJson = new JSONObject();
        fullJson.put("id", idField);
        fullJson.put("match", matchField);
        fullJson.put("out_port", outPortField);
        fullJson.put("flow-name", flowNameField);
        fullJson.put("instructions", insinstructionsField);
        fullJson.put("priority", priorityField);
        fullJson.put("hard-timeout", timeOutField);
        fullJson.put("table_id", "0");


        //System.out.println(dstMac);

        String fullField = "[" + fullJson.toString() + "]";
        fullJson.clear();
        fullJson.put("flow", fullField);

        System.out.println(fullJson.toString());
        result = fullJson.toString();

        return result;
    }


}
