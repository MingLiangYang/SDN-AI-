# Internet2实验数据说明



### 金同学负责收集的数据有：1. 边缘交换机的流表统计信息、2.  各个交换机上送的PacketIn



## 边缘交换机：

​	IP域边缘交换机s1、s7、s9、s10、s12和服务器边缘交换机s15

## 文件目录：

-- data_yyyy_mm_dd

​		-- packetIn

​					-- openflow_1_pktin.txt

​					-- openflow_2_pktin.txt

​					-- openflow_3_pktin.txt

​					-- ...

​					-- openflow_17_pktin.txt

​		-- s1_yyyy-mm-dd_rtxpacket.txt

​		-- s7_yyyy-mm-dd_rtxpacket.txt

​		-- s9_yyyy-mm-dd_rtxpacket.txt

​		-- s10_yyyy-mm-dd_rtxpacket.txt

​		-- s12_yyyy-mm-dd_rtxpacket.txt

​		-- s15_yyyy-mm-dd_rtxpacket.txt

其中packetIn文件夹内是s1-s17的交换机上送的PacketIn，sx_yyyy-mm-dd_rtxpacket.txt是边缘交换机sx的流表统计信息。

## 某个交换机上送的PacketIn文件格式

* "Time"   毫秒时间戳   "src_IP"   源IP   "dst_IP"   目的IP   "EtherType"   协议类型   "srcProt"   源port "dstPort"   目的port   "size"   包长度

```
Time 1603018786088 src_IP 10.0.0.3 dst_IP 10.0.0.1 EtherType 0x0800 srcProt 2048 dstPort 6047 size 98
Time 1603018786091 src_IP 10.0.0.1 dst_IP 10.0.0.3 EtherType 0x0800 srcProt 0 dstPort 8095 size 98
Time 1603018816754 src_IP 10.0.0.3 dst_IP 10.0.0.10 EtherType 0x0800 srcProt 2048 dstPort 50642 size 98
Time 1603019778929 src_IP 38.98.19.126 dst_IP 192.168.4.121 EtherType 0x0800 srcProt 80 dstPort 52146 size 66
```



## 某个边缘交换机的流表统计文件格式

##### PS: 文件记录了该交换机每一秒的流表统计信息，使用ovs-ofctl dump-flow br0获取的。在某一秒使用ovs-ofctl dump-flow br0得到的流表统计格式如下：

* 第一行是毫秒时间戳

```
1603026574160  
```

* 第二行是无关的消息 

```
NXST_FLOW reply (xid=0x4):
```

* 之后会有n行，每行表示一条流表在当前时刻的统计值（其中第一行流表和最后一行不用管，那两条是lldp流表和drop流表。中间的IP流表是有用的。）

###### 第一行lldp流表不用管

```
cookie=0x2b00000000000005, duration=7836.336s, table=0, n_packets=4702, n_bytes=405940, idle_age=1, priority=100,dl_type=0x88cc actions=CONTROLLER:65535
```

**IP流表**：cookie、持续时间、tableID、**包数、比特数**、空闲时间、idle_age、优先级、**ip、入端口、源IP域、	目的IP域、出端口**

```
cookie=0x2a0000000000063a, duration=720.909s, table=0, n_packets=58, n_bytes=21298, idle_timeout=300, idle_age=275, priority=10,ip,in_port=3,nw_src=38.0.0.0/8,nw_dst=192.0.0.0/8 actions=output:1
```

```
cookie=0x2a00000000000692, duration=373.486s, table=0, n_packets=45, n_bytes=4633, idle_timeout=300, idle_age=149, priority=10,ip,in_port=1,nw_src=192.0.0.0/8,nw_dst=38.0.0.0/8 actions=output:3
```

###### 最后一行drop流表不用管

```
cookie=0x1b00000000000005, duration=7836.336s, table=0, n_packets=8220, n_bytes=1293179, idle_age=19, priority=0 actions=CONTROLLER:65535
```








