package zhijianglab;

import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import zhijianglab.Flowentries;

public class FlowTable
{
    private int tableId = 0;
    private int pktLookUp = 0;
    private int pktMatched = 0;
    private int activeFlows = 0;
    private ArrayList<Flowentries> flowentries = new ArrayList<Flowentries>();

    public FlowTable(JSONObject tableObject)
    {
        String tableStaticInfo = tableObject.optString("opendaylight-flow-table-statistics:flow-table-statistics");
        JSONObject tableStaticObject = JSONObject.fromObject(tableStaticInfo);
        this.tableId = tableObject.optInt("id");
        this.pktLookUp = tableStaticObject.optInt("packets-looked-up");
        this.pktMatched = tableStaticObject.optInt("packets-matched");
        this.activeFlows = tableStaticObject.optInt("active-flows");

        if (this.pktLookUp == 0 && this.pktMatched == 0)
        {
            return;
        }
        //System.out.println(tableStaticObject.toString());
        String flowEntriesInfo = tableObject.optString("flow");
        //System.out.println(flowEntriesInfo);


        JSONArray flowEntriesArray = JSONArray.fromObject(flowEntriesInfo);

        for (int i = 0; i < flowEntriesArray.size(); i++)
        {
            JSONObject perFlowEntryObject = flowEntriesArray.getJSONObject(i);
            Flowentries newFlowEntry = new Flowentries(perFlowEntryObject);
            flowentries.add(newFlowEntry);
            //newFlowEntry.showInfo();
        }




    }

    public int getPktLookUp()
    {
        return pktLookUp;
    }

    public int getPktMatched()
    {
        return pktMatched;
    }

    public void showInfo(String path)
    {
        FileWriter fw = new FileWriter(path);

        fw.writeLine("tableId " + this.tableId);
        fw.writeLine("pktLookUp " + this.pktLookUp);
        fw.writeLine("pktMatched " + this.pktMatched);
        fw.writeLine("flow_entries_information");


        //System.out.println("########## table : " + this.tableId + " info ##########");
        //System.out.println("tableId : " + this.tableId);
        //System.out.println("pktLookUp : " + this.pktLookUp);
        //System.out.println("pktMatched : " + this.pktMatched);

        //System.out.println("######### show flowentries info #########");
        //System.out.println();

        for (int i = 0; i < flowentries.size(); i++)
        {
            flowentries.get(i).showInfo(path);
        }
        //System.out.println();

        //System.out.println("######### flowentries info end #########");

        //System.out.println("######### show flowentries info #########");

        //System.out.println("########## table : "+ this.tableId +" info end ##########");
        //System.out.println();
    }
}
