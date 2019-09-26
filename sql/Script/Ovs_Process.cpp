#include<iostream>
#include<vector>
#include<math.h>
#include<map>
#include<time.h>
#include<algorithm>
#include<io.h>
using namespace std;
#define filePathD  "D:\\Mysql\\sql\\ovs_source\\data"
#define filePathC  "D:\\Mysql\\sql\\ovs_source\\CPU"
#define deltT 10
struct Port
{
	long long time;
	string switchid;
	string portid;
	int PacketReceivedAll;
	int PacketReceivedSuccess;
	int PacketsTransmittedAll;
	int PacketTransmittedSuccess;
	int ByteReceivedAll;
	int ByteTransmittedAll;
}; 
struct Ovs_info
{
	long long time;
	string Name;
	string CPU;
	string Memory;
}; 
struct IP_info
{
	long long time;
	string Switchid;
	string Src_ip;
	string Dst_ip;
};
struct Time//用来计算攻击时间 
{
	int year;
	int month;
	int day;
	int hour;
	int minute;
	int second; 
};
long long PortBeginTime;
FILE *Tf,*IPf;
vector<long long>AttackTime;
vector<Time> PretreatTime;
vector<Port> Pre,Cur;
vector<Ovs_info> GlobalOvs_CPU;
vector<Ovs_info> GlobalOvs_Entropy;
vector<string> files; 
vector<IP_info> DeltSet;//一段时间间隔内的IP包集合，用来计算熵值 

void getFiles( string path, vector<string>& files );
long long ReturnTime(int year,int month,int day,int hour,int minute,int second);
bool JudgeAttack(long long time);
void GetTimeAttack();
void GetTimeRecord();

long long StrToLInt(string s);//把字符串型数据转为整型并返回整型结果
int StrToInt(string s);

int FindSwitch(Port P);//查找对应的交换机 
string GetString(FILE *f);//从文件中读出一个字符串，并返回读到的字符串 
void CalculateEntropy();

void Process1Logic(FILE *f,FILE *resP,FILE *resS);
void GainRecord(long long time,FILE *resP,FILE *resS);
void IP_Process(FILE *f,string fileName);
void Process3Logic(FILE *f,string fileName); //处理交换机的性能数据，得到CPU和内存的信息等（目前为止） 

int main()
{
	FILE *fp1,*fp2,*fp3,*fp4,*resp,*ress,*resovs;//fp1对应于存储端口收到转发数据包的信息，fp2对应数据包源端口、目的端口的信息。以下代码中标识“1”和标识“2”代表的东西也如此 
	if((fp1=fopen("ovs_source\\port_stats.txt","r"))==NULL)
	{
		printf("The file port_stats can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((Tf=fopen("ovs_source\\time.txt","r"))==NULL) {
	printf("The file time.txt can not be open!");
	exit(1);//结束程序的执行
	}
	GetTimeRecord();
	GetTimeAttack();
	//cout<<AttackTime.size();
	if((resp=fopen("ovs_result\\ovs_port.txt","w"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((ress=fopen("ovs_result\\ovs_switch.txt","w"))==NULL)
	{
		printf("The file ovs_switch.txt can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((IPf=fopen("ovs_result\\ovs_ip_info.txt","w"))==NULL)
	{
		printf("The file ovs_ip_info.txt can not be open!");
    	exit(1);//结束程序的执行
	}
	getFiles(filePathD, files);
	for(int i=0;i<files.size();i++)//处理IP信息
	{
		if((fp3=fopen(files[i].c_str(),"r"))==NULL) {
			printf("The file data can not be open!");
			exit(1);//结束程序的执行
		}
		cout<<files[i]<<endl;
		while(!feof(fp3)){IP_Process(fp3,files[i]);} 
	}
	
	files.clear();
	getFiles(filePathC, files);
	for(int i=0;i<files.size();i++)//处理CPU数据 
	{
		if((fp4=fopen(files[i].c_str(),"r"))==NULL) {
			printf("The file data can not be open!");
			exit(1);//结束程序的执行
		}
		cout<<files[i]<<endl;
		while(!feof(fp4)){Process3Logic(fp4,files[i]);}
	}
	while(!feof(fp1)){Process1Logic(fp1,resp,ress);}//处理port_stat
	return 0;
} 
void Process3Logic(FILE *f,string fileName)//得到关于交换机的信息：CPU和内存。其中交换机的名字要自己指定，现在指定的是openflow：1（此处可能以后要改） 
{
	Ovs_info tempOvs;
	long long  tempt=0,pretime=0;
	while(!feof(f))
	{
		tempt=StrToLInt(GetString(f));
		tempOvs.time=tempt;
		tempOvs.CPU=GetString(f);
		tempOvs.Memory=GetString(f);
		tempOvs.Name="openflow:8796749338201";
		GlobalOvs_CPU.push_back(tempOvs);
	}
} 
void IP_Process(FILE *f,string fileName)//交换机的名字要自己指定，现在指定的是openflow：1（此处可能以后要改） 
{
	IP_info temp_ip;
	string temps;
	long long pretime=0;
	while(!feof(f))
	{
		temp_ip.Switchid="openflow:8796749338201";
		temp_ip.time=StrToLInt(GetString(f));
		temp_ip.Src_ip=GetString(f);
		temp_ip.Dst_ip=GetString(f);
		while(fgetc(f)!='\n'&&!feof(f));
		fgetc(f);
		//cout<<temp_ip.time<<" "<<temp_ip.Src_ip<<"  "<<temp_ip.Dst_ip<<endl;
		if(temp_ip.time-pretime>=deltT)
		{
			CalculateEntropy();
			pretime=temp_ip.time;
			DeltSet.clear();
		}
		DeltSet.push_back(temp_ip);
	}
//	cout<<"out call"<<endl;
	CalculateEntropy();
	DeltSet.clear();
} 
void CalculateEntropy()//计算熵值函数，完成 （没有加入tag标签） 
{
	int i;
	double Entropy=0;
	map<string,vector<IP_info> > IPMap;
	map<string ,int> SIP;
	map<string,vector<IP_info> >::iterator Diter;
	map<string,int>::iterator Siter;
//	cout<<DeltSet.size()<<endl;
	for(i=0;i<DeltSet.size();i++)
	{
		if(IPMap.find(DeltSet[i].Dst_ip)==IPMap.end())
		{
			vector<IP_info> temp;
			temp.push_back(DeltSet[i]);
			IPMap.insert(pair<string, vector<IP_info> >(DeltSet[i].Dst_ip,temp));
		}
		else IPMap[DeltSet[i].Dst_ip].push_back(DeltSet[i]);
	}
	for(Diter=IPMap.begin();Diter!=IPMap.end();Diter++)
	{
		Entropy=0;SIP.clear();
	//	cout<<Diter->first<<"  : ";
		for(i=0;i<Diter->second.size();i++)
		{
			if(SIP.find(Diter->second[i].Src_ip)==SIP.end())
			{
				SIP.insert(pair<string,int>(Diter->second[i].Src_ip,1));
			}
			else SIP[Diter->second[i].Src_ip]++;
		}
		for(Siter=SIP.begin();Siter!=SIP.end();Siter++) {Entropy+=(double)Siter->second/Diter->second.size()*log((double)Siter->second/Diter->second.size());}
		for(i=0;i<Diter->second.size();i++)
		{
			//cout<<Diter->second[i].time<<endl;
			if(Diter->second[i].time!=0)//防止原始数据最后一行出现回车的情况
			{ 
			fprintf(IPf,"%lld,%s,%s,%s,%.5f,",Diter->second[i].time,Diter->second[i].Switchid.c_str(),Diter->second[i].Src_ip.c_str(),Diter->second[i].Dst_ip.c_str(),Entropy);
			if(JudgeAttack(Diter->second[i].time)) fprintf(IPf,"1\n");
			else fprintf(IPf,"0\n");
			} 
		}
	}
	IPMap.clear();
//	cout<<endl;
}
void Process1Logic(FILE *f,FILE *resP,FILE *resS)//处理得到交换机和端口的数据表 
{
	Port tempP;
	long long pretime=0,curtime;
	string temps;
	//TODO 找到数据起始时间，然后再将文件指针放到文件开始 
	while(!feof(f))
	{
		GetString(f);temps=GetString(f);curtime=StrToLInt(temps);tempP.time=curtime/1000;//将时间转化为秒 
		GetString(f);tempP.switchid=GetString(f);
		GetString(f);tempP.portid=GetString(f);
		GetString(f);tempP.PacketReceivedAll=StrToInt(GetString(f));
		GetString(f);tempP.PacketReceivedSuccess=StrToInt(GetString(f));
		GetString(f);tempP.PacketsTransmittedAll=StrToInt(GetString(f));
		GetString(f);tempP.PacketTransmittedSuccess=StrToInt(GetString(f));
		GetString(f);tempP.ByteReceivedAll=StrToInt(GetString(f));
		GetString(f);tempP.ByteTransmittedAll=StrToInt(GetString(f));
		if(curtime-pretime>1000||feof(f))//因为当前数据的时间间隔不固定，只知道大概是3秒，所以当两条数据的时间间隔小于1秒时，都认为是同一秒的数据。然后计算时的时间间隔取数据之间的3秒间隔，所以在此处不用人为修改 
		{
			GainRecord(pretime,resP,resS);
			pretime=curtime;
			Cur.clear();
		}
		//cout<<tempP.time<<endl;
		Cur.push_back(tempP);
	}
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
			tempP.time=Cur[i].time;
			tempP.switchid=Cur[i].switchid;
			tempP.PacketReceivedAll=Cur[i].PacketReceivedAll-Pre[j].PacketReceivedAll;
			tempP.PacketReceivedSuccess=Cur[i].PacketReceivedSuccess-Pre[j].PacketReceivedSuccess;
			tempP.PacketsTransmittedAll=Cur[i].PacketsTransmittedAll-Pre[j].PacketsTransmittedAll;
			tempP.PacketTransmittedSuccess=Cur[i].PacketTransmittedSuccess-Pre[j].PacketTransmittedSuccess;
			tempP.ByteReceivedAll=Cur[i].ByteReceivedAll-Pre[j].ByteReceivedAll;
			tempP.ByteTransmittedAll=Cur[i].ByteTransmittedAll-Pre[j].ByteTransmittedAll;
			fprintf(resP,"%lld,%s,%d,%d,%d,%d,%d,%d,",time,Cur[i].portid.c_str(),tempP.PacketReceivedAll,tempP.PacketReceivedSuccess,tempP.PacketsTransmittedAll,tempP.PacketTransmittedSuccess,tempP.ByteReceivedAll,tempP.ByteTransmittedAll);
			if(JudgeAttack(tempP.time))fprintf(resP,"1\n");
			else fprintf(resP,"0\n");
			Pre[j]=Cur[i];
		}
		else
		{
			tempP.time=Cur[i].time;
			tempP.switchid=Cur[i].switchid;
			tempP.PacketReceivedAll=Cur[i].PacketReceivedAll;
			tempP.PacketReceivedSuccess=Cur[i].PacketReceivedSuccess;
			tempP.PacketsTransmittedAll=Cur[i].PacketsTransmittedAll;
			tempP.PacketTransmittedSuccess=Cur[i].PacketTransmittedSuccess;
			tempP.ByteReceivedAll=Cur[i].ByteReceivedAll;
			tempP.ByteTransmittedAll=Cur[i].ByteTransmittedAll;
			fprintf(resP,"%lld,%s,%d,%d,%d,%d,%d,%d,",time,Cur[i].portid.c_str(),Cur[i].PacketReceivedAll,Cur[i].PacketReceivedSuccess,Cur[i].PacketsTransmittedAll,Cur[i].PacketTransmittedSuccess,Cur[i].ByteReceivedAll,Cur[i].ByteTransmittedAll);
			if(JudgeAttack(tempP.time))fprintf(resP,"1\n");
			else fprintf(resP,"0\n");
			Pre.push_back(Cur[i]);
		}
		if(flag1)
		{
			
			Switch[k].time=tempP.time;
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
		int indexi=0;
		//TODO CPUi的值也要写到文件中去，暂时用0代替 
		//cout<<Switch[k].time<<"  "<<Switch[k].switchid<<endl; 
		indexi=FindSwitch(Switch[k]);
		//cout<<indexi<<endl;
		if(indexi!=-1)
		{
			fprintf(resS,"%lld,%s,%d,%d,%d,%d,%d,%d,%s,%s,",time,Switch[k].switchid.c_str(),Switch[k].PacketReceivedAll,Switch[k].PacketReceivedSuccess,Switch[k].PacketsTransmittedAll,Switch[k].PacketTransmittedSuccess,Switch[k].ByteReceivedAll,Switch[k].ByteTransmittedAll,GlobalOvs_CPU[indexi].CPU.c_str(),GlobalOvs_CPU[indexi].Memory.c_str());
			if(JudgeAttack(Switch[k].time))fprintf(resS,"1\n");
			else fprintf(resS,"0\n");
		}
		else
		{
			fprintf(resS,"%lld,%s,%d,%d,%d,%d,%d,%d,0.00,0.00,",time,Switch[k].switchid.c_str(),Switch[k].PacketReceivedAll,Switch[k].PacketReceivedSuccess,Switch[k].PacketsTransmittedAll,Switch[k].PacketTransmittedSuccess,Switch[k].ByteReceivedAll,Switch[k].ByteTransmittedAll);
			if(JudgeAttack(Switch[k].time))fprintf(resS,"1\n");
			else fprintf(resS,"0\n");
		}
	}
}




int FindSwitch(Port P)
{
	//cout<<GlobalOvs_CPU.size()<<endl;
	for(int i=0;i<GlobalOvs_CPU.size();i++)
	{
		if(P.time==GlobalOvs_CPU[i].time&&P.switchid==GlobalOvs_CPU[i].Name)return i;
	}
	return -1;
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
void GetTimeAttack()//得到攻击时间段 
{
	
	int t1=65*60,t2=t1+23*60,t3=t2+4*60,t4=t3+21*60,t5=t4+8*60,t6=t5+17*60,t7=t6+600,t8=t7+13*60,t9=t8+229*60,t10=t9+1200;
	int year=2019,month=8;
	//起始时间需要从PretreatTime中读取 
	long long BeginTime=ReturnTime(year,month,PretreatTime[0].day,PretreatTime[0].hour,PretreatTime[0].minute,PretreatTime[0].second);
	//以下攻击时间是固定的，每次只用修改起始时间即可 
	AttackTime.push_back(BeginTime+t1);
	AttackTime.push_back(BeginTime+t2);
	
	AttackTime.push_back(BeginTime+t3);
	AttackTime.push_back(BeginTime+t4);

	AttackTime.push_back(BeginTime+t5);
	AttackTime.push_back(BeginTime+t6);
	
	AttackTime.push_back(BeginTime+t7);
	AttackTime.push_back(BeginTime+t8);
	
	AttackTime.push_back(BeginTime+t9);
	AttackTime.push_back(BeginTime+t10);
	
	//以下攻击时间是随机设定的，每次需要从 PretreatTime中读取 
	for(int i=1;i<PretreatTime.size();i+=2)
	{
		AttackTime.push_back(ReturnTime(year,month,PretreatTime[i].day,PretreatTime[i].hour,PretreatTime[i].minute,PretreatTime[i].second));
		AttackTime.push_back(ReturnTime(year,month,PretreatTime[i+1].day,PretreatTime[i+1].hour,PretreatTime[i+1].minute,PretreatTime[i+1].second));
	}
//	for(int i=0;i<AttackTime.size();i++)cout<<AttackTime[i]<<endl; 
} 
void GetTimeRecord()//从文件中读取每次随机设定的攻击时间，要根据time.txt文件的格式修改此函数 
{
	string temps;
	Time tempt;
	while(!feof(Tf))
	{	GetString(Tf);
		GetString(Tf);GetString(Tf);GetString(Tf);GetString(Tf);
		temps=GetString(Tf);tempt.day=StrToInt(temps);
		temps=GetString(Tf);tempt.hour=(temps[0]-'0')*10+temps[1]-'0';
		tempt.minute=(temps[3]-'0')*10+temps[4]-'0';
		tempt.second=(temps[6]-'0')*10+temps[7]-'0';
	//	cout<<tempt.day<<" "<<tempt.hour<<" "<<tempt.minute<<" "<<tempt.second<<" "<<endl;
		GetString(Tf);
		PretreatTime.push_back(tempt);
	}
}
bool JudgeAttack(long long time)
{
	for(int i=0;i<AttackTime.size();i+=2)
	{
		if(time<=AttackTime[i+1]&&time>=AttackTime[i])return true;
	}
	return false;
} 
long long ReturnTime(int year,int month,int day,int hour,int minute,int second)
{
    time_t timep;
    struct tm *p;
    p = localtime(&timep);
    p->tm_year=year-1900;
    p->tm_mon=month-1;
	p->tm_mday=day;
	p->tm_hour=hour;
	p->tm_min=minute;
	p->tm_sec=second;
    //printf("%d %d %d %d %d %d %d %d %d\n",p->tm_year,p->tm_mon,p->tm_mday,p->tm_hour,p->tm_min,p->tm_sec,p->tm_wday,p->tm_yday,p->tm_isdst);
    timep = mktime(p);
    //printf("time()->localtime()->mktime():%ld\n", timep);
    //scanf(" ");
    return timep;
}
void getFiles( string path, vector<string>& files ) {
	//文件句柄
	long   hFile   =   0;
	//文件信息
	//cout<<endl<<" path:"<<path<<endl;
	struct _finddata_t fileinfo;
	string p;
	if((hFile = _findfirst(p.assign(path).append("\\*").c_str(),&fileinfo)) !=  -1) {
		do {
			//如果是目录,迭代之
			//如果不是,加入列表
			if((fileinfo.attrib &  _A_SUBDIR)) {
				if(strcmp(fileinfo.name,".") != 0  &&  strcmp(fileinfo.name,"..") != 0)
					getFiles( p.assign(path).append("\\").append(fileinfo.name), files );
			} else {
				files.push_back(p.assign(path).append("\\").append(fileinfo.name) );
			}
		} while(_findnext(hFile, &fileinfo)  == 0);
		_findclose(hFile);
	}
}
