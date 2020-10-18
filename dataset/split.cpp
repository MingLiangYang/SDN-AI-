#include <pcap/pcap.h>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fstream> 
#include <map>
#include <vector>

using namespace std;


typedef unsigned long long u_int64;
typedef u_int32_t u_int32;
typedef u_int16_t u_int16;
typedef u_int8_t u_int8;
typedef pair<u_int64, u_int64> PII;

// 10.0.0.11
u_int8 serverIP[4];

map<string, PII> dict;

char * fname = "/Users/hyliu/DataSets/testbed-14jun.pcap";
char * file_1 = "1-50.pcap";
char * file_2 = "51-100.pcap";
char * file_3 = "101-150.pcap";
char * file_4 = "151-200.pcap";
char * file_5 = "201-254.pcap";

u_int64 field1_first = 1276484468;
u_int64 field2_first = 1276484468;
u_int64 field3_first = 1276484468;
u_int64 field4_first = 1276484467;
u_int64 field5_first = 1276484468;

// bool field1_first_flag = true;
// bool field2_first_flag = true;
// bool field3_first_flag = true;
// bool field4_first_flag = true;
// bool field5_first_flag = true;
char * field1_lable = "field1_lable.txt";
char * field2_lable = "field2_lable.txt";
char * field3_lable = "field3_lable.txt";
char * field4_lable = "field4_lable.txt";
char * field5_lable = "field5_lable.txt";

ofstream outfile1;
ofstream outfile2;
ofstream outfile3;
ofstream outfile4;
ofstream outfile5;


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

vector<string> split(const string& str, const string& delim) {  
    vector<string> res;  
    if("" == str) return res;  

    char * strs = new char[str.length() + 1] ; 
    strcpy(strs, str.c_str());   
 
    char * d = new char[delim.length() + 1];  
    strcpy(d, delim.c_str());  
 
    char *p = strtok(strs, d);  
    while(p) {  
        string s = p; 
        res.push_back(s); 
        p = strtok(NULL, d);  
    }  
 
    return res;  
}

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

int attack_pkt_cnt = 0;
int main()
{
	serverIP[0] = 10;
	serverIP[1] = 0;
	serverIP[2] = 0;
	serverIP[3] = 11;

	gen_attack_flow_map("res2.txt");

	// return 0;
	char errbuf[256];
	pcap_t * pcap;
	pcap = pcap_open_offline(fname, errbuf);			
	struct pcap_pkthdr pkthdr;								

	struct etherHeader_t *etherHeader;
	struct ipHeader_t *ipHeader;
	struct tcpHeader_t *tcpHeader;
	struct udpHeader_t *udpHeader;

	int size_ethernet = sizeof(struct etherHeader_t);
	int size_ip = sizeof(struct ipHeader_t);
	int size_tcp = sizeof(struct tcpHeader_t);
	int size_udp = sizeof(struct udpHeader_t);

	pcap_t * handler = pcap_open_dead(1, 65535);

	pcap_dumper_t *pdumper1 = pcap_dump_open(handler, file_1);
	pcap_dumper_t *pdumper2 = pcap_dump_open(handler, file_2);
	pcap_dumper_t *pdumper3 = pcap_dump_open(handler, file_3);
	pcap_dumper_t *pdumper4 = pcap_dump_open(handler, file_4);
	pcap_dumper_t *pdumper5 = pcap_dump_open(handler, file_5);

	outfile1.open(field1_lable, ios::app);
	outfile2.open(field2_lable, ios::app);
	outfile3.open(field3_lable, ios::app);
	outfile4.open(field4_lable, ios::app);
	outfile5.open(field5_lable, ios::app);

	while (1)
	{
		const u_char *pktStr = pcap_next(pcap, &pkthdr);
		// printf("timestamp: %ld.%6d\n", pkthdr.ts.tv_sec, pkthdr.ts.tv_usec);
		if (pktStr == NULL)
		{
			printf("pcap end!\n");
			break;
		} else {

			etherHeader = (struct etherHeader_t*)(pktStr);

			ipHeader = (struct ipHeader_t*)(pktStr + size_ethernet);

			string four_tuple = to_string(ipHeader->srcIP[0]) + "." +\
								to_string(ipHeader->srcIP[1]) + "." +\
								to_string(ipHeader->srcIP[2]) + "." +\
								to_string(ipHeader->srcIP[3]) + \
								to_string(ipHeader->dstIP[0]) + "." +\
								to_string(ipHeader->dstIP[1]) + "." +\
								to_string(ipHeader->dstIP[2]) + "." +\
								to_string(ipHeader->dstIP[3]);


			if (ipHeader->protocol == 0x06) {
				tcpHeader = (struct tcpHeader_t*)(pktStr + size_ip + size_ethernet);

				four_tuple += to_string(tcpHeader->srcPort[0] * 256 + tcpHeader->srcPort[1]);
				four_tuple += to_string(tcpHeader->dstPort[0] * 256 + tcpHeader->dstPort[1]);
			} else if (ipHeader->protocol == 0x11) {
				udpHeader = (struct udpHeader_t*)(pktStr + size_ip + size_ethernet);
				four_tuple += to_string(udpHeader->srcPort[0] * 256 + udpHeader->srcPort[1]);
				four_tuple += to_string(udpHeader->dstPort[0] * 256 + udpHeader->dstPort[1]);
			}
			u_int64 this_pkt_tsp = pkthdr.ts.tv_sec;
			// cout << dict[four_tuple].first << " " << dict[four_tuple].second << endl;
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
			else {
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
			}

			

		}
	}
	outfile1.close();
	outfile2.close();
	outfile3.close();
	outfile4.close();
	outfile5.close();
	pcap_close(pcap);
	return 0;
}