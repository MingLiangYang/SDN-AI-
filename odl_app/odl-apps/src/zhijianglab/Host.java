package zhijianglab;

public class Host
{
    private int id = 0;
    private String hostId = null;
    private String ipAddr = null;
    private String macAddr = null;
    public Host(int id, String ip, String mac, String hostId)
    {
        this.id = id;
        this.ipAddr = ip;
        this.macAddr = mac;
        this.hostId = hostId;
    }
    public void showInfo()
    {
        System.out.println("this is host" + id);
        System.out.println("ip : " + ipAddr);
        System.out.println("mac : " + macAddr);
        System.out.println("host-id : " + hostId);
        System.out.println();
    }
}
