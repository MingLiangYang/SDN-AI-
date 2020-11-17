package org.jbh.flowcontroller.impl.packethandler.utils;

import java.util.HashMap;
import java.util.Map;

public class FixedFlow {

    // fix_flow 规定了以下三个流在具体的ovs上的出端口  注意大写
    private static final String FIXED_FLOW_1 = "08:00:27:6F:AA:01-08:00:27:6F:AA:05";
    private static final String FIXED_FLOW_2 = "08:00:27:6F:AA:02-08:00:27:6F:AA:0A";
    private static final String FIXED_FLOW_3 = "08:00:27:6F:AA:03-08:00:27:6F:AA:0B";

    private static final String TEST_FIXED_FLOW_1 = "82:37:BA:2C:41:3F-42:B0:A7:D6:5A:69";

    private static final String FIXED_FLOW_1_back = "08:00:27:6F:AA:05-08:00:27:6F:AA:01";
    private static final String FIXED_FLOW_2_back = "08:00:27:6F:AA:0A-08:00:27:6F:AA:02";
    private static final String FIXED_FLOW_3_back = "08:00:27:6F:AA:0B-08:00:27:6F:AA:03";

    private Map<String, Map<String,String>> FIXED_FLOW_TO_NODE_MAP = new HashMap<>();

    public FixedFlow(){
        int ovsIDArr1[] = {1,4,5,6,8,9};
        int ovsPIDArr1[] = {2,2,2,3,3,3};
        addFixedFlow(FIXED_FLOW_1, ovsIDArr1, ovsPIDArr1);

        int ovsIDArr2[] = {3,7,5,6,16,15,14};
        int ovsPIDArr2[] = {3,2,2,2,2,1,4};
        addFixedFlow(FIXED_FLOW_2, ovsIDArr2, ovsPIDArr2);

        int ovsIDArr3[] = {7,5,6,16,15};
        int ovsPIDArr3[] = {2,2,2,2,4};
        addFixedFlow(FIXED_FLOW_3, ovsIDArr3, ovsPIDArr3);

        int testOvsIDArr1[] = {1,4,5,6,8,9};
        int testOvsPIDArr1[] = {2,4,2,3,4,3};
        addFixedFlow(TEST_FIXED_FLOW_1, testOvsIDArr1, testOvsPIDArr1);

        int backOvsIDArr1[] = {1,4,5,6,8,9};
        int backOvsPIDArr1[] = {4,1,1,1,1,1};
        addFixedFlow(FIXED_FLOW_1_back, backOvsIDArr1, backOvsPIDArr1);

        int backOvsIDArr2[] = {3,7,5,6,16,15,14};
        int backOvsPIDArr2[] = {4,1,3,1,1,2,3};
        addFixedFlow(FIXED_FLOW_2_back, backOvsIDArr2, backOvsPIDArr2);

        int backOvsIDArr3[] = {7,5,6,16,15};
        int backOvsPIDArr3[] = {4,3,1,1,2};
        addFixedFlow(FIXED_FLOW_3_back, backOvsIDArr3, backOvsPIDArr3);
    }

    public Map<String, Map<String,String>> getFIXED_FLOW_TO_NODE_MAP(){
        return FIXED_FLOW_TO_NODE_MAP;
    }

    /**
     * saved Fixed Flow to FIXED_FLOW_TO_NODE_MAP
     * @param FIXED_FLOW "mac-mac"
     * @param ovsIDArr {1,2} equal to "openflow:1,openflow:2"
     * @param ovsPIDArr {1,2} equal to "openflow:1:1, openflow:2:2"
     */
    public void addFixedFlow(String FIXED_FLOW, int[] ovsIDArr, int[] ovsPIDArr){
        HashMap<String,String> FIXED_FLOW_NODE_MAP = new HashMap<>();
        for(int i = 0; i < ovsIDArr.length; ++i){
            FIXED_FLOW_NODE_MAP.put("openflow:" + ovsIDArr[i], "openflow:" + ovsIDArr[i] + ":" + ovsPIDArr[i]);
        }
        FIXED_FLOW_TO_NODE_MAP.put(FIXED_FLOW, FIXED_FLOW_NODE_MAP);
    }
}
