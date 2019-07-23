package zhijianglab;

import net.sf.json.JSONObject;

public class Match
{
    private boolean isIpMatch = false;
    private String ipDestAddr = null;


    public Match(JSONObject matchObject)
    {
        String ipdst = matchObject.optString("ipv4-destination");
        if (ipdst.isEmpty())
        {
            isIpMatch = false;
            ipDestAddr = null;
        }
        else
        {
            isIpMatch = true;
            ipDestAddr = ipdst;
        }
    }

    public void showInfo()
    {
        if (isIpMatch)
        {
            System.out.println(ipDestAddr);
        }
        else
        {
            System.out.println("this is not ip match entry");
        }
    }
}
