package zhijianglab;

import zhijianglab.OdlUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main
{
    public static void main(String args[])throws IOException
    {
        OdlUtil ou = new OdlUtil("127.0.0.1", 8181, "admin", "admin");
        ou.getTopo();
        ou.parseTopo();
        ou.showAllSwitchs();
        //ou.setFlowEntry();

//        Flowentries fe = new Flowentries();
//
//        Map<String, String> matches = new HashMap<String,String>();
//        Map<String, String> actions = new HashMap<String,String>();
//        matches.put("src_mac", "16:72:47:e4:8f:cf");
//        matches.put("dst_mac", "18:66:da:17:9d:e9");
//        matches.put("id", "523");
//        matches.put("out_port", "2");
//        matches.put("flow-name", "test");
//        matches.put("priority", "1000");
//        matches.put("src_ip", "10.0.0.1/32");
//        matches.put("dst_ip", "10.0.0.2/32");
//        matches.put("eth-type", "0x0800");
//        //matches.put("timeout", "10");
//        actions.put("output", "3");
//        actions.put("controller", "controller");
//        fe.genEntryJson(matches, actions);
//
//        ou.setFlowEntry("openflow:26830024711657", 0, 523, matches, actions);
    }
}

