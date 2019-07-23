#include<iostream>
#include<vector>
#include<math.h>
#include<map>
using namespace std;
#define delT 5
#define TINF 9e8
struct SubRecord//deltaT自己决定，Hi是计算得到 
{
	long long time;
	string src_ip;
	string dst_ip;
	int src_port;
	int dst_port;
	int packet_total;
	int packet_suc;
	int byte_sum;
	float CPUi;
	string Si;
};
vector<SubRecord> Record,ResultR;
map<string,int> IPSrcMap;//统计每一个ip地址出现的个数，计算熵值时使用
map<int,int>PortSrcMap;//统计每一个端口出现的个数，计算熵值时使用
map<string,int>::iterator Iter;
map<int,int>::iterator IterInt;

long long StrToInt(string s);//把字符串型数据转为整型并返回整型结果
string GetString(FILE *f);//从文件中读出一个字符串，并返回读到的字符串 
void Process1Logic(FILE *f);
void Process2Logic(FILE *f,FILE *resf);
void ProcessPortInfo(FILE *f);
int main()
{
	FILE *fp1,*fp2,*resfp;//fp1对应于存储端口收到转发数据包的信息，fp2对应数据包源端口、目的端口的信息。以下代码中标识“1”和标识“2”代表的东西也如此 
	if((fp1=fopen("openflow.txt","r"))==NULL)//packet_in是处理之前的文件名 
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((fp2=fopen("mydebug","r"))==NULL)//packet_in是处理之前的文件名 //TODO 文件名 
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((resfp=fopen("ovs_result/ovs_vector.txt","w"))==NULL)//packet_in是处理之前的文件名 
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	while(!feof(fp1)){Process1Logic(fp1);}//读文件 
	while(!feof(fp2)){Process2Logic(fp2,resfp);}//读文件 
	return 0;
} 
void Process2Logic(FILE *f,FILE *resf)
{
	int i=0,begintime,j,k=0,PktNum=0;
	double temp1,temp2,PortSrcEntropy=0,IPSrcEntropy=0;
	bool flag; 
	string temps;
	SubRecord SR;
	fprintf(resf,"Timetag,src_ip,dst_ip,src_port,deltaT,packet_total,packet_suc,byte_sum,Hi,CPUi,Si\n");
	while(!feof(f))
	{
		temps=GetString(f); SR.time=StrToInt(temps);
		temps=GetString(f); SR.src_ip=temps;
		temps=GetString(f); SR.dst_ip=temps;
		temps=GetString(f); SR.src_port=(int)StrToInt(temps);
		temps=GetString(f); SR.dst_port=(int)StrToInt(temps);
		fscanf(f,"%f %f",&temp1,&temp2);
		SR.CPUi=temp1+temp2;
		fscanf(f,"%f %f\n",&temp1,&temp2);
		//temps=GetString(f); SR.CPUi=(float)StrToInt(temps);
		Record.push_back(SR);
	}
	SR.time=TINF;
	Record.push_back(SR);
   	//for(i=0;i<Record.size();i++) cout<<i<<" "<<Record[i].time<<endl;
	for(i=0,begintime=Record[0].time;i<Record.size();i++)
	{
		if(Record[i].time-begintime<delT)
		{
			PktNum++;
			for(j=0,flag=false;j<ResultR.size();j++)
			{
				if((ResultR[j].src_ip==Record[i].src_ip)&&(ResultR[j].dst_ip==Record[i].dst_ip))
				{
					ResultR[j].packet_total++;flag=true;
					break;
				}
			}
			if(!flag){SR=Record[i];SR.packet_total=1;ResultR.push_back(SR);}
			
			if(IPSrcMap.find(Record[i].src_ip)==IPSrcMap.end()) {
				IPSrcMap.insert(pair<string,int>(Record[i].src_ip,1));
			} else IPSrcMap[Record[i].src_ip]++;
			if(PortSrcMap.find(Record[i].src_port)==PortSrcMap.end()) {
				PortSrcMap.insert(pair<int,int>(Record[i].src_port,1));
			} else PortSrcMap[Record[i].src_port]++;
		}
		else//一个时间间隔结束 //熵值计算，如果分母为0，则不进行除法，结果直接为0 
		{
			cout<<PktNum<<" ";
			begintime+=delT;
			if(i!=Record.size()-1)i--;
			for(Iter=IPSrcMap.begin(); Iter!=IPSrcMap.end(); Iter++)IPSrcEntropy+=((double)Iter->second/PktNum)*log((double)Iter->second/PktNum);
			for(IterInt=PortSrcMap.begin(); IterInt!=PortSrcMap.end(); IterInt++) PortSrcEntropy+=((double)IterInt->second/PktNum)*log((double)IterInt->second/PktNum);
			//cout<<IPSrcEntropy<<" "<<PortSrcEntropy<<endl;
			for(int j=0;j<ResultR.size();j++)
			{
				if(PortSrcEntropy!=0)fprintf(resf,"%d-%d,%s,%s,%d,%d,%d,,,%.5f,%f,\n",begintime-delT,begintime,ResultR[j].src_ip.c_str(),ResultR[j].dst_ip.c_str(),ResultR[j].src_port,delT,ResultR[j].packet_total,IPSrcEntropy/PortSrcEntropy,ResultR[j].CPUi);
				else fprintf(resf,"%d-%d,%s,%s,%d,%d,%d,,,0.00000,%f,\n",begintime-delT,begintime,ResultR[j].src_ip.c_str(),ResultR[j].dst_ip.c_str(),ResultR[j].src_port,delT,ResultR[j].packet_total,ResultR[j].CPUi);//cout<<"time="<<ResultR[j].time<<"  "<<ResultR[j].src_ip<<"  "<<ResultR[j].dst_ip<<"  "<<ResultR[j].packet_total<<"  "<<ResultR[j].CPUi<<endl;
			}
			PktNum=0;
			PortSrcMap.clear();
			IPSrcMap.clear();
			IPSrcEntropy=0;
			PortSrcEntropy=0;
			ResultR.clear(); 
		}
	}
}
void Process1Logic(FILE *f)
{
	string TagS;
	while(!feof(f))
	{
		TagS=GetString(f);
		if(TagS=="prots_information")
		{
		//	TagS=ProcessPortInfo(f);
		}
		else if(TagS=="table_information")
		{
			//Todo//ProcessTableInfo(f);
		}
		else if(TagS=="flow_entries_information")
		{
			//Todo//ProcessFlowInfo(f);
		}
	}
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
long long StrToInt(string s) {
	long long result=0;
	for(int i=0; i<s.size(); i++) {
		result+=(s[i]-'0')*pow(10,s.size()-1-i);
	}
	return result;
}
