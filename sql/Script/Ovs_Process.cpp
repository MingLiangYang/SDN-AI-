#include<iostream>
#include<vector>
#include<math.h>
#include<map>
#include<algorithm>
using namespace std;
#define filePath  "D:\\Mysql\\sql\\ovs_src_normal"
#define filePathR  "D:\\Mysql\\sql\\ovs_result"
#define delT 5
#define TINF 9e8
struct Port
{
	string switchid;
	string portid;
	int PacketReceivedAll;
	int PacketReceivedSuccess;
	int PacketsTransmittedAll;
	int PacketTransmittedSuccess;
	int ByteReceivedAll;
	int ByteTransmittedAll;
}; 
struct IP_info
{
	string switchid;
	string Src_ip;
	string Dst_ip;
	string Inport;
	string OutPort;
	int Packet;
	int Byte;
};
vector<IP_info> PreIP,CurIP;
vector<Port> Pre,Cur;
long long StrToLInt(string s);//把字符串型数据转为整型并返回整型结果
int StrToInt(string s);
bool Judge(IP_info a,IP_info b);
string GetString(FILE *f);//从文件中读出一个字符串，并返回读到的字符串 
void Process1Logic(FILE *f,FILE *resP,FILE *resS);
void GainRecord(long long time,FILE *resP,FILE *resS);
void GainRecord_IP(long long pretime,FILE *resf);
void Process2Logic(FILE *f,FILE *resf);
int main()
{
	FILE *fp1,*fp2,*resp,*ress,*resip;//fp1对应于存储端口收到转发数据包的信息，fp2对应数据包源端口、目的端口的信息。以下代码中标识“1”和标识“2”代表的东西也如此 
	if((fp1=fopen("ovs_src_normal\\port_stats.txt","r"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((fp2=fopen("ovs_src_normal\\flow_stats.txt","r"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((resp=fopen("ovs_result\\ovs_port.txt","w"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((ress=fopen("ovs_result\\ovs_switch.txt","w"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((resip=fopen("ovs_result\\ovs_ip_info.txt","w"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	while(!feof(fp1)){Process1Logic(fp1,resp,ress);}//处理port_stat
	while(!feof(fp2)){Process2Logic(fp2,resip);}//处理flow_stat
	return 0;
} 
void Process2Logic(FILE *f,FILE *resf)
{
	IP_info tempi;
	long long pretime=0,curtime;
	string temps;
	
	while(!feof(f))
	{
		GetString(f);temps=GetString(f);curtime=StrToLInt(temps);
		GetString(f);tempi.switchid=GetString(f);
		GetString(f);tempi.Src_ip=GetString(f);
		GetString(f);tempi.Dst_ip=GetString(f);
		GetString(f);tempi.Inport=GetString(f);
		GetString(f);tempi.OutPort=GetString(f);
		GetString(f);tempi.Packet=StrToInt(GetString(f));
		GetString(f);tempi.Byte=StrToInt(GetString(f));
		if(curtime-pretime>1000||feof(f))
		{
			GainRecord_IP(pretime,resf);
			pretime=curtime;
		}
		CurIP.push_back(tempi);
	}
}

void Process1Logic(FILE *f,FILE *resP,FILE *resS)
{
	Port tempP;
	long long pretime=0,curtime;
	string temps;
	while(!feof(f))
	{
		GetString(f);temps=GetString(f);curtime=StrToLInt(temps);
		GetString(f);tempP.switchid=GetString(f);
		GetString(f);tempP.portid=GetString(f);
		GetString(f);tempP.PacketReceivedAll=StrToInt(GetString(f));
		GetString(f);tempP.PacketReceivedSuccess=StrToInt(GetString(f));
		GetString(f);tempP.PacketsTransmittedAll=StrToInt(GetString(f));
		GetString(f);tempP.PacketTransmittedSuccess=StrToInt(GetString(f));
		GetString(f);tempP.ByteReceivedAll=StrToInt(GetString(f));
		GetString(f);tempP.ByteTransmittedAll=StrToInt(GetString(f));
		if(curtime-pretime>1000||feof(f))
		{
			GainRecord(pretime,resP,resS);
			pretime=curtime;
		}
		Cur.push_back(tempP);
	}
}
bool Judge(IP_info a,IP_info b)
{
	if(a.switchid==b.switchid&&a.Src_ip==b.Src_ip&&a.Dst_ip==b.Dst_ip&&a.Inport==b.Inport&&a.OutPort==b.OutPort) return true;
	else return false;
}
void GainRecord_IP(long long pretime,FILE *resf)
{
	int i,j,k,PktNum=0;
	double SipE=0,DipE=0;
	bool flag;
	map<string,int> SipM,DipM;
	map<string,int>::iterator Iter;
	vector<IP_info> tempV;
	IP_info tempi;
	for(i=0;i<CurIP.size();i++)
	{
		flag=false;
		for(j=0;j<PreIP.size();j++)
		{
			if(Judge(PreIP[j],CurIP[i]))
			{
				flag=true;
				break;
			}
		}
		if(flag)
		{
			tempi=CurIP[i];
			tempi.Packet-=PreIP[j].Packet;
			tempi.Byte-=PreIP[j].Byte;
			PreIP[j]=CurIP[i];
		}
		else
		{
			tempi=CurIP[i];
			PreIP.push_back(tempi);
		}
		PktNum+=tempi.Packet;
		if(SipM.find(tempi.Src_ip)==SipM.end()) {SipM.insert(pair<string,int>(tempi.Src_ip,tempi.Packet));} else SipM[tempi.Src_ip]+=tempi.Packet;
		if(DipM.find(tempi.Dst_ip)==DipM.end()) {DipM.insert(pair<string,int>(tempi.Dst_ip,tempi.Packet));} else DipM[tempi.Dst_ip]+=tempi.Packet;
		tempV.push_back(tempi);
	}
	for(Iter=SipM.begin();Iter!=SipM.end();Iter++) if(Iter->second!=0) SipE+=((double)Iter->second/PktNum)*log((double)Iter->second/PktNum);
	for(Iter=DipM.begin();Iter!=DipM.end();Iter++) if(Iter->second!=0) DipE+=((double)Iter->second/PktNum)*log((double)Iter->second/PktNum);
	for(k=0;k<tempV.size();k++)
	//TODO 考虑当某个数据包数量为0时不再输出 
	fprintf(resf,"%lld,%s,%s,%s,%s,%s,%.5f,%.5f,%d,%d\n",pretime,tempV[k].switchid.c_str(),tempV[k].Src_ip.c_str(),tempV[k].Dst_ip.c_str(),tempV[k].Inport.c_str(),tempV[k].OutPort.c_str(),SipE,DipE,tempV[k].Packet,tempV[k].Byte);
	CurIP.clear();
}
void GainRecord(long long time,FILE *resP,FILE *resS)//time 是为了给每一条记录加一个时间戳 
{
	int i,j,k;
	vector<Port> Switch;
	Port tempP;
	bool flag=false,flag1=false;
	for(i=0;i<Cur.size();i++)
	{
		flag=false;flag1=false;
		for(j=0;j<Pre.size();j++)
		{
			if(Pre[j].portid==Cur[i].portid)
			{
				flag=true;
				break;
			}
		} 
		for(k=0;k<Switch.size();k++)
		{
			if(Switch[k].switchid==Cur[i].switchid)
			{
				flag1=true;
				break;
			}
		}
		if(flag)
		{
			tempP.switchid=Cur[i].switchid;
			tempP.PacketReceivedAll=Cur[i].PacketReceivedAll-Pre[j].PacketReceivedAll;
			tempP.PacketReceivedSuccess=Cur[i].PacketReceivedSuccess-Pre[j].PacketReceivedSuccess;
			tempP.PacketsTransmittedAll=Cur[i].PacketsTransmittedAll-Pre[j].PacketsTransmittedAll;
			tempP.PacketTransmittedSuccess=Cur[i].PacketTransmittedSuccess-Pre[j].PacketTransmittedSuccess;
			tempP.ByteReceivedAll=Cur[i].ByteReceivedAll-Pre[j].ByteReceivedAll;
			tempP.ByteTransmittedAll=Cur[i].ByteTransmittedAll-Pre[j].ByteTransmittedAll;
			fprintf(resP,"%lld,%s,%d,%d,%d,%d,%d,%d\n",time,Cur[i].portid.c_str(),tempP.PacketReceivedAll,tempP.PacketReceivedSuccess,tempP.PacketsTransmittedAll,tempP.PacketTransmittedSuccess,tempP.ByteReceivedAll,tempP.ByteTransmittedAll);
			Pre[j]=Cur[i];
		}
		else
		{
			tempP.switchid=Cur[i].switchid;
			tempP.PacketReceivedAll=Cur[i].PacketReceivedAll;
			tempP.PacketReceivedSuccess=Cur[i].PacketReceivedSuccess;
			tempP.PacketsTransmittedAll=Cur[i].PacketsTransmittedAll;
			tempP.PacketTransmittedSuccess=Cur[i].PacketTransmittedSuccess;
			tempP.ByteReceivedAll=Cur[i].ByteReceivedAll;
			tempP.ByteTransmittedAll=Cur[i].ByteTransmittedAll;
			fprintf(resP,"%lld,%s,%d,%d,%d,%d,%d,%d\n",time,Cur[i].portid.c_str(),Cur[i].PacketReceivedAll,Cur[i].PacketReceivedSuccess,Cur[i].PacketsTransmittedAll,Cur[i].PacketTransmittedSuccess,Cur[i].ByteReceivedAll,Cur[i].ByteTransmittedAll);
			Pre.push_back(Cur[i]);
		}
		if(flag1)
		{
			Switch[k].PacketReceivedAll+=tempP.PacketReceivedAll;
			Switch[k].PacketReceivedSuccess+=tempP.PacketReceivedSuccess;
			Switch[k].PacketsTransmittedAll+=tempP.PacketsTransmittedAll;
			Switch[k].PacketTransmittedSuccess+=tempP.PacketTransmittedSuccess;
			Switch[k].ByteReceivedAll+=tempP.ByteReceivedAll;
			Switch[k].ByteTransmittedAll+=tempP.ByteTransmittedAll;
		}
		else
		{
			Switch.push_back(tempP);
		}
	}
	for(k=0;k<Switch.size();k++)
	{
		//TODO CPUi的值也要写到文件中去，暂时用0代替 
		fprintf(resS,"%lld,%s,%d,%d,%d,%d,%d,%d,1.00\n",time,Switch[k].switchid.c_str(),Switch[k].PacketReceivedAll,Switch[k].PacketReceivedSuccess,Switch[k].PacketsTransmittedAll,Switch[k].PacketTransmittedSuccess,Switch[k].ByteReceivedAll,Switch[k].ByteTransmittedAll);
	}
	Cur.clear();
}
string GetString(FILE *f)
{
	char c;string temps="";
	while((c=fgetc(f))!=EOF&&c!=' '&&c!='\n')
	{
		temps+=c;
	}
	return temps;
}
long long StrToLInt(string s) {
	long long result=0;
	for(int i=0; i<s.size(); i++) {
		result+=(s[i]-'0')*pow(10,s.size()-1-i);
	}
	return result;
}
int StrToInt(string s) {
	int result=0;
	for(int i=0; i<s.size(); i++) {
		result+=(s[i]-'0')*pow(10,s.size()-1-i);
	}
	return result;
}
