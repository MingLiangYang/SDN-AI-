## 数据集说明文档

0. 数据集来源
1. 数据集解析
2. 数据集标签
3. 数据集重放

### 0. 数据集来源

- [数据集](http://205.174.165.80/CICDataset/ISCX-IDS-2012/Dataset/testbed-14jun.pcap)
- [标签](http://205.174.165.80/CICDataset/ISCX-IDS-2012/Dataset/labeled_flows_xml.zip)

### 1. 数据集解析

数据集解析代码为`split.cpp`，主要采用[libpcap](https://www.tcpdump.org/)库。编译时需要链接pcap库。

> libpacp库函数可以参考man page，这里只对实现思路进行说明

#### step1: 数据集读入内存

```c++
pcap_t * pcap;
pcap = pcap_open_offline(fname, errbuf);
```

#### step2: 获取pcap文件中数据包句柄

```C++
const u_char *pktStr = pcap_next(pcap, &pkthdr);
```

>  如果pcap读取完毕`pcap_next`返回NULL

#### step3: 用包头结构体解析数据包

包头结构体

```c++
struct etherHeader_t
{ //Pcap捕获的数据帧头
    u_int8 dstMAC[6]; 		//目的MAC地址
    u_int8 srcMAC[6]; 		//源MAC地址
    u_int16 frameType;    	//帧类型
};

//IP数据报头
struct ipHeader_t
{ //IP数据报头
    u_int8 Ver_HLen;       	//版本+报头长度
    u_int8 TOS;           	//服务类型
    u_int8 TotalLen[2];     //总长度
    u_int8 ID[2]; 			//标识
    u_int8 Flag_Segment[2]; //标志+片偏移
    u_int8 ttl;            	//生存周期
    u_int8 protocol;       	//协议类型
    u_int8 checksum[2];     //头部校验和
    u_int8 srcIP[4]; 		//源IP地址
    u_int8 dstIP[4]; 		//目的IP地址
};

//TCP数据报头
struct tcpHeader_t
{ //TCP数据报头
    u_int8 srcPort[2];		//源端口
    u_int8 dstPort[2];		//目的端口
    u_int8 SeqNO[4];			//序号
    u_int8 AckNO[4]; 			//确认号
    u_int8 HeaderLen; 		//数据报头的长度(4 bit) + 保留(4 bit)
    u_int8 Flags; 			//标识TCP不同的控制消息
    u_int8 Window[2]; 		//窗口大小
    u_int8 checksum[2]; 		//校验和
    u_int8 UrgentPointer[2];  //紧急指针
};

//UDP数据
struct udpHeader_t
{
    u_int8 srcPort[2];     	// 源端口号16bit
    u_int8 dstPort[2];    	// 目的端口号16bit
    u_int8 len[2];        	// 数据包长度16bit
    u_int8 checkSum[2];   	// 校验和16bit
};
```

因为数据包句柄类型为uchar，故可以使用强制转换的方式获取各层头部指针：

```c++
etherHeader = (struct etherHeader_t*)(pktStr);
ipHeader = (struct ipHeader_t*)(pktStr + size_ethernet);
if (ipHeader->protocol == 0x06) {
				tcpHeader = (struct tcpHeader_t*)(pktStr + size_ip + size_ethernet);
			} else if (ipHeader->protocol == 0x11) {
				udpHeader = (struct udpHeader_t*)(pktStr + size_ip + size_ethernet);
			}

```

#### step4: IP域划分，与最终数据集生成

基于提取出的源IP字段，进行IP域划分：

```c++
if (ipHeader->srcIP[0] >= 1 && ipHeader->srcIP[0] <= 50) {
					// if (field1_first_flag) {
					// 	field1_first_flag = false;
					// 	field1_first = pkthdr.ts.tv_sec;
					// }
					//outfile1 << this_pkt_tsp - field1_first << endl;
					pcap_dump((u_char*)pdumper1, &pkthdr, pktStr);
					pcap_dump_flush(pdumper1);
				} else if (ipHeader->srcIP[0] >= 51 && ipHeader->srcIP[0] <= 100) {
					// if (field2_first_flag) {
					// 	field2_first_flag = false;
					// 	field2_first = pkthdr.ts.tv_sec;
					// }
					//outfile2 << this_pkt_tsp - field2_first << endl;
					pcap_dump((u_char*)pdumper2, &pkthdr, pktStr);
					pcap_dump_flush(pdumper2);
				} else if (ipHeader->srcIP[0] >= 101 && ipHeader->srcIP[0] <= 150) {
					// if (field3_first_flag) {
					// 	field3_first_flag = false;
					// 	field3_first = pkthdr.ts.tv_sec;
					// }
					//outfile3 << this_pkt_tsp - field3_first << endl;
					pcap_dump((u_char*)pdumper3, &pkthdr, pktStr);
					pcap_dump_flush(pdumper3);
				} else if (ipHeader->srcIP[0] >= 151 && ipHeader->srcIP[0] <= 200) {
					// if (field4_first_flag) {
					// 	field4_first_flag = false;
					// 	field4_first = pkthdr.ts.tv_sec;
					// }
					//outfile4 << this_pkt_tsp - field4_first << endl;
					pcap_dump((u_char*)pdumper4, &pkthdr, pktStr);
					pcap_dump_flush(pdumper4);
				} else if (ipHeader->srcIP[0] >= 201 && ipHeader->srcIP[0] <= 254) {
					// if (field5_first_flag) {
					// 	field5_first_flag = false;
					// 	field5_first = pkthdr.ts.tv_sec;
					// }
					//outfile5 << this_pkt_tsp - field5_first << endl;
					pcap_dump((u_char*)pdumper5, &pkthdr, pktStr);
					pcap_dump_flush(pdumper5);
				}
```

其中，`pcap_dump`,`pcap_dump_flush`用于写入pcap文件，也就是最终的五个pcap

### 2. 数据集标签

#### step1: XML解析

> XML解析脚本为`xml_parser.py`，使用minidom解析器

主要通过`getElementsByTagName`方法获取XML标签中的值

```python
srcIP = flow.getElementsByTagName('source')[0].childNodes[0].data
dstIP = flow.getElementsByTagName('destination')[0].childNodes[0].data
srcPort = flow.getElementsByTagName('sourcePort')[0].childNodes[0].data
dstPort = flow.getElementsByTagName('destinationPort')[0].childNodes[0].data
starttime = unix_time(flow.getElementsByTagName('startDateTime')[0].childNodes[0].data)
endtime = unix_time(flow.getElementsByTagName('stopDateTime')[0].childNodes[0].data)
```

得到的标签数据有：

- 攻击流的源ip；
- 攻击流的目的ip；
- 攻击流的源端口；
- 攻击流的目的端口；
- 攻击流的起始时间；
- 攻击流的结束时间。

最后将标签数据按照`%s %s %s %s %s %s\n' % (srcIP, dstIP, srcPort, dstPort, starttime, endtime`的格式输出到`res2.txt`文件中，方便后续的处理。

```
192.168.2.112 131.202.243.84 4558 5555 1276462757 1276462850
192.168.2.112 131.202.243.84 1562 5555 1276466357 1276466450
192.168.2.112 131.202.243.84 2030 5555 1276469957 1276470050
192.168.2.112 131.202.243.84 2680 5555 1276473557 1276473650
192.168.2.112 131.202.243.84 3801 5555 1276477157 1276477250
```

#### step2: 基于攻击流的信息，生成标签

> split.cpp

从py脚本生成的`res2.txt`文件中，读入攻击流的信息。并生成字典，key为四元组，value为<开始时间，结束时间>：

```c++
void gen_attack_flow_map(char * file_name) {
	ifstream in("res2.txt");
    string line;
    if (in) {  
        while (getline (in, line)) {   
            vector<string> flow_meta = split(line, " ");
            // for (auto i : flow_meta) {
            // 	cout << i << endl;
            // }
            // cout << endl;

            string four_tuple = flow_meta[0] + flow_meta[1] +\
            					flow_meta[2] + flow_meta[3];
            // cout << four_tuple << endl;
            PII timeval;
            timeval.first  = (u_int64)(atol(flow_meta[4].c_str()));
            timeval.second = (u_int64)(atol(flow_meta[5].c_str()));
            dict[four_tuple] = timeval;
        }
    }
    return;
}
```

有了这个字典之后，我们就可以对每个数据包进行检测：如果数据包的四元组可以在字典中找到对应的攻击流，并且数据包的时间戳在<开始时间，结束时间>区间内。就可以记录这个数据包出现的时间戳，并与数据集中第一个数据包的时间戳相减获得时间间隔，在最后打标签的时候，就可以利用重放开始时间+时间间隔的方式，精准定位攻击流出现的时间。此外，由于实验的限制，可以用`memcpy`将攻击包的目的地址修改为服务器地址`10.0.0.11`。

```c++
if (dict.find(four_tuple) != dict.end()) {

	memcpy(ipHeader->dstIP, serverIP, 4);
	if (ipHeader->srcIP[0] >= 1 && ipHeader->srcIP[0] <= 50) {
					// if (field1_first_flag) {
					// 	field1_first_flag = false;
					// 	field1_first = pkthdr.ts.tv_sec;
					// }
					outfile1 << this_pkt_tsp - field1_first << endl;
					pcap_dump((u_char*)pdumper1, &pkthdr, pktStr);
					pcap_dump_flush(pdumper1);
				} else if (ipHeader->srcIP[0] >= 51 && ipHeader->srcIP[0] <= 100) {
					// if (field2_first_flag) {
					// 	field2_first_flag = false;
					// 	field2_first = pkthdr.ts.tv_sec;
					// }
					outfile2 << this_pkt_tsp - field2_first << endl;
					pcap_dump((u_char*)pdumper2, &pkthdr, pktStr);
					pcap_dump_flush(pdumper2);
				} else if (ipHeader->srcIP[0] >= 101 && ipHeader->srcIP[0] <= 150) {
					// if (field3_first_flag) {
					// 	field3_first_flag = false;
					// 	field3_first = pkthdr.ts.tv_sec;
					// }
					outfile3 << this_pkt_tsp - field3_first << endl;
					pcap_dump((u_char*)pdumper3, &pkthdr, pktStr);
					pcap_dump_flush(pdumper3);
				} else if (ipHeader->srcIP[0] >= 151 && ipHeader->srcIP[0] <= 200) {
					// if (field4_first_flag) {
					// 	field4_first_flag = false;
					// 	field4_first = pkthdr.ts.tv_sec;
					// }
					outfile4 << this_pkt_tsp - field4_first << endl;
					pcap_dump((u_char*)pdumper4, &pkthdr, pktStr);
					pcap_dump_flush(pdumper4);
				} else if (ipHeader->srcIP[0] >= 201 && ipHeader->srcIP[0] <= 254) {
					// if (field5_first_flag) {
					// 	field5_first_flag = false;
					// 	field5_first = pkthdr.ts.tv_sec;
					// }
					outfile5 << this_pkt_tsp - field5_first << endl;
					pcap_dump((u_char*)pdumper5, &pkthdr, pktStr);
					pcap_dump_flush(pdumper5);
				}
			} 
```

### 4. 数据集重放

#### step1: 使用tcpreplay

```shell
//        <pcap path>  <interface>
tcpreplay 1-50.pacp -i enp0s3
```

