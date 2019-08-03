#include <pcap/pcap.h>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "filewriter.h"

using namespace std;


typedef unsigned long long u_int64;
typedef u_int32_t u_int32;
typedef u_int16_t u_int16;
typedef u_int8_t u_int8;


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
    u_int8 TotalLen[2];     	//总长度
    u_int8 ID[2]; 		//标识
    u_int8 Flag_Segment[2]; 	//标志+片偏移
    u_int8 ttl;            	//生存周期
    u_int8 protocol;       	//协议类型
    u_int8 checksum[2];     	//头部校验和
    u_int8 srcIP[4]; 		//源IP地址
    u_int8 dstIP[4]; 		//目的IP地址
};

//TCP数据报头
struct tcpHeader_t
{ //TCP数据报头
    u_int8 srcPort[2];		//源端口
    u_int8 dstPort[2];		//目的端口
    u_int8 SeqNO[4];		//序号
    u_int8 AckNO[4]; 		//确认号
    u_int8 HeaderLen; 		//数据报头的长度(4 bit) + 保留(4 bit)
    u_int8 Flags; 		//标识TCP不同的控制消息
    u_int8 Window[2]; 		//窗口大小
    u_int8 checksum[2]; 	//校验和
    u_int8 UrgentPointer[2];    //紧急指针
};

//UDP数据
struct udpHeader_t
{
    u_int8 srcPort[2];     	// 源端口号16bit
    u_int8 dstPort[2];    	// 目的端口号16bit
    u_int8 len[2];        	// 数据包长度16bit
    u_int8 checkSum[2];   	// 校验和16bit
};

char writeBuffer[10000] = {0};
Filewriter ff;
char filePath[100] = {0};

//数据包五元组
struct fiveTuple_t
{
	u_int8 srcIP[4];	//源IP地址
	u_int8 dstIP[4]; 	//目的IP地址
	u_int8 protocol;       	//协议类型
	u_int8 srcPort[2];     	//源端口号16bit
	u_int8 dstPort[2];    	//目的端口号16bit
	u_char str[13];
	void printinfo(struct pcap_pkthdr* header, int number)
	{
		//
		memset(filePath, 0, sizeof(filePath));
		sprintf(filePath, "/home/ovs/dataset/wednesday/data/result%d.txt", number);



		sprintf(writeBuffer, "Time %ld ", header->ts.tv_sec);
		ff.writeToFile(writeBuffer, filePath);
		memset(writeBuffer, 0, sizeof(writeBuffer));
		sprintf(writeBuffer, "src_IP %d.%d.%d.%d ", srcIP[0], srcIP[1], srcIP[2], srcIP[3]);
		ff.writeToFile(writeBuffer, filePath);
		memset(writeBuffer, 0, sizeof(writeBuffer));
		sprintf(writeBuffer, "dst_IP %d.%d.%d.%d ", dstIP[0], dstIP[1], dstIP[2], dstIP[3]);
		ff.writeToFile(writeBuffer, filePath);
		memset(writeBuffer, 0, sizeof(writeBuffer));
		sprintf(writeBuffer, "EtherType 0x0800 ");
		ff.writeToFile(writeBuffer, filePath);
		memset(writeBuffer, 0, sizeof(writeBuffer));
		int srcP = srcPort[0] * 256 + srcPort[1];
		int dstP = dstPort[0] * 256 + dstPort[1];
		sprintf(writeBuffer, "srcPort %d ",srcP);
		ff.writeToFile(writeBuffer, filePath);
		memset(writeBuffer, 0, sizeof(writeBuffer));
		sprintf(writeBuffer, "dstPort %d ",dstP);
		ff.writeToFile(writeBuffer, filePath);
		memset(writeBuffer, 0, sizeof(writeBuffer));
		sprintf(writeBuffer, "size %d\n", header->len);
		ff.writeToFile(writeBuffer, filePath);
		memset(writeBuffer, 0, sizeof(writeBuffer));
	}
	void tochar()
	{
		str[0]  = (u_char)(srcIP[0]);
		str[1]  = (u_char)(srcIP[1]);
		str[2]  = (u_char)(srcIP[2]);
		str[3]  = (u_char)(srcIP[3]);

		str[4]  = (u_char)(dstIP[0]);
		str[5]  = (u_char)(dstIP[1]);
		str[6]  = (u_char)(dstIP[2]);
		str[7]  = (u_char)(dstIP[3]);

		str[8]  = (u_char)(protocol);

		str[9]  = (u_char)(srcPort[0]);
		str[10] = (u_char)(srcPort[1]);

		str[11] = (u_char)(dstPort[0]);
		str[12] = (u_char)(dstPort[1]);

	}
};


char errbuf[PCAP_ERRBUF_SIZE];


class extracter
{
private:
	u_int64 pktCounter = 0;
public:
	u_int64 getCnt()
	{
		return this->pktCounter;
	}
	void extract(char * fname, struct fiveTuple_t *fiveTupleFuf,u_int64 n, int number);
};


void extracter::extract(char * fname, struct fiveTuple_t *fiveTupleFuf,u_int64 n, int number)
{

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

	struct pcap_pkthdr* header;
	const u_char* pktStr;

	while ((pcap_next_ex(pcap, &header, &pktStr) > 0))
	{
		//printf("1\n");
		int flag = 0;
		//this->pktCounter++;
		fiveTuple_t temp;
		etherHeader = (struct etherHeader_t*)(pktStr);
		ipHeader = (struct ipHeader_t*)(pktStr + size_ethernet);
		//printf("1\n");
		if (etherHeader->frameType == 0x08)
		{
			flag = 1;
		}
	
		memcpy(temp.srcIP,ipHeader->srcIP,4);
		memcpy(temp.dstIP,ipHeader->dstIP,4);
		temp.protocol = ipHeader->protocol;


		if (ipHeader->protocol == 0x06)
		{

			//printf("this is a tcp packet !\n");
			tcpHeader = (struct tcpHeader_t*)(pktStr + size_ip + size_ethernet);

			memcpy(temp.srcPort,tcpHeader->srcPort,2);
			memcpy(temp.dstPort,tcpHeader->dstPort,2);

		}
		else if (ipHeader->protocol == 0x11)
		{
			
			udpHeader = (struct udpHeader_t*)(pktStr + size_ip + size_ethernet);
			memcpy(temp.srcPort,udpHeader->srcPort,2);
			memcpy(temp.dstPort,udpHeader->dstPort,2);
		}
		
		temp.tochar();

		if (this->pktCounter == 0)
		{
			//printf("2\n");
			this->pktCounter++;
			fiveTupleFuf[1] = temp;


			memcpy(fiveTupleFuf[1].srcIP,temp.srcIP,4);
			memcpy(fiveTupleFuf[1].dstIP,temp.dstIP,4);
			fiveTupleFuf[1].protocol = temp.protocol;
			memcpy(fiveTupleFuf[1].srcPort,temp.srcPort,2);
			memcpy(fiveTupleFuf[1].dstPort,temp.dstPort,2);

			fiveTupleFuf[this->pktCounter].tochar();

			//fiveTupleFuf[1].printinfo();
			//printf("1\n");
			//printf("\n");
		}
		else
		{
			
			//printf("1\n");
			this->pktCounter++;
				
			memcpy(fiveTupleFuf[this->pktCounter].srcIP,temp.srcIP,4);
			memcpy(fiveTupleFuf[this->pktCounter].dstIP,temp.dstIP,4);
			fiveTupleFuf[this->pktCounter].protocol = temp.protocol;
			memcpy(fiveTupleFuf[this->pktCounter].srcPort,temp.srcPort,2);
			memcpy(fiveTupleFuf[this->pktCounter].dstPort,temp.dstPort,2);


			fiveTupleFuf[this->pktCounter].tochar();
			
		}
		if (flag == 1)
			fiveTupleFuf[this->pktCounter].printinfo(header, number);
		if(this->pktCounter == n)
		{
			break;
		}

	}
	pcap_close(pcap);
}
