//该程序每次处理文件，往数据库里写数据时，都会把原来的数据删除，要想实现追加功能，需要把数据表项里的index删除。然后用时间戳Ticktime作为主键。 

#include<iostream>
#include<vector>
#include<map>
#include<math.h>
#include<io.h>
#include<time.h>
#include<algorithm>
using namespace std;
#define TINF 9e13
#define delT 2
#define filePath1  "D:\\Mysql\\sql\\packet_in_src_attack"
#define filePath  "D:\\Mysql\\sql\\packet_in_src_normal"
#define BEGIN 34  //BEGIN 代表filepath的字符数，用来生成目标文件名 
struct SubRecord {
	long long time;
	string src_ip;
	string dst_ip;
	int src_port;
	int dst_port;
	int size;
};
FILE *gf;
vector<long long> AttackTime;
vector<string> files;
vector<SubRecord> Record,PartRecord;
map<string,int> IPSrcMap,IPDstMap,HIP;//统计每一个ip地址出现的个数，计算熵值时使用
map<int,int>PortSrcMap,PortDstMap;//统计每一个端口出现的个数，计算熵值时使用
map<string,int>::iterator Iter;
map<int,int>::iterator IterInt;
long long ReturnTime(int year,int month,int day,int hour,int minute,int second);
bool cmp(SubRecord a,SubRecord b){return a.time<b.time;}
int StrToInt(string s);//把字符串型数据转为整型并返回整型结果
long long StrToLong(string s);
string GetString(FILE *f);//从文件中读出一个字符串，并返回读到的字符串
void ProcessOneLine(FILE *f,FILE *tempf);//处理文件中的一行记录，得到一个结构体（SubRecord），并把结构体压入向量Record中，同时将该结构体的每一个成员写入中间数据文件
void GainRecord(FILE *f,FILE *res,vector<SubRecord> Record);//处理向量Record，计算熵值，得到最终每条记录的各项数据，并将数据存入f指向的文件中//注意：要求Record中的记录是按照time升序排列 //Tag是0代表正常流量，Tag是1代表攻击流量 
void getFiles( string path, vector<string>& files );
void CreateTable(string temps,FILE *SQLfp);
void GetTimeAttack();
int main() {
	int i=0,j=0,AttackRecord;
	SubRecord Tail;
	Tail.time=TINF;
	FILE *fp,*resfp,*tempfp,*SQLfp,*AIfp;

	getFiles(filePath, files);
	GetTimeAttack();
	if((resfp=fopen("packet_in_result\\packet_in_vector.txt","w"))==NULL) {
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
	if((tempfp=fopen("packet_in_result\\packet_in_info.txt","w"))==NULL) {
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
//	if((gf=fopen("packet_in_result\\packet_in_test.txt","w"))==NULL) {
//		printf("The file can not be open!");
//		exit(1);//结束程序的执行
//	}
	if((SQLfp=fopen("Mysql.sql","w"))==NULL) {
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
	fputs("time,src_ip,dst_ip,src_port,dst_port,size\n",tempfp);//中间数据文件
	for(int i=0; i<files.size(); i++) {
		PartRecord.clear();
		if((fp=fopen(files[i].c_str(),"r"))==NULL) {
			printf("The file can not be open!");
			exit(1);//结束程序的执行
		}
		while(!feof(fp)) {
			ProcessOneLine(fp,tempfp);
		}
		PartRecord.push_back(Tail);
		FILE *tempf;
		string temps="packet_in_result\\";
		for(j=BEGIN; j<files[i].size(); j++)temps+=files[i][j];
		tempf=fopen(temps.c_str(),"w");
		cout<<"1: "<<temps<<endl;
		GainRecord(tempf,resfp,PartRecord);
		CreateTable(temps,SQLfp);
	}
	CreateTable("packet_in_result\\packet_in_vector.txt",SQLfp);
	
	cout<<"over，导入文件：packet_in_vector.txt至数据库";
	return 0;
}
void GetTimeAttack()//得到攻击时间段 
{
	AttackTime.push_back(ReturnTime(2019,8,15,20,47,0));
	AttackTime.push_back(ReturnTime(2019,8,15,21,10,0));
	
	AttackTime.push_back(ReturnTime(2019,8,15,21,14,0));
	AttackTime.push_back(ReturnTime(2019,8,15,21,35,0));
	
	AttackTime.push_back(ReturnTime(2019,8,15,21,43,0));
	AttackTime.push_back(ReturnTime(2019,8,15,22,0,0));
	
	AttackTime.push_back(ReturnTime(2019,8,15,22,10,0));
	AttackTime.push_back(ReturnTime(2019,8,15,22,23,0));
	
	AttackTime.push_back(ReturnTime(2019,8,15,23,21,59));
	AttackTime.push_back(ReturnTime(2019,8,15,23,38,56));
	
	AttackTime.push_back(ReturnTime(2019,8,15,23,53,56));
	AttackTime.push_back(ReturnTime(2019,8,16,4,13,1));
	
	AttackTime.push_back(ReturnTime(2019,8,16,4,38,1));
	AttackTime.push_back(ReturnTime(2019,8,16,4,50,49));
}
bool JudgeAttack(long long time)
{
	for(int i=0;i<AttackTime.size();i+=2)
	{
		if(time<=AttackTime[i+1]&&time>=AttackTime[i])return true;
	}
	return false;
} 
void ProcessOneLine(FILE *f,FILE *tempf) {
	int i;
	string Str;
	SubRecord SR;
	GetString(f);
	Str=GetString(f);
	SR.time=StrToLong(Str);//现在给的packet_in的单位到了毫秒。所以要除以1000转化为秒 
	GetString(f);
	SR.src_ip=GetString(f);
	GetString(f);
	SR.dst_ip=GetString(f);
	for(i=0; i<3; i++)GetString(f);
	SR.src_port=StrToInt(GetString(f));
	GetString(f);
	SR.dst_port=StrToInt(GetString(f));
	GetString(f);
	SR.size=StrToInt(GetString(f));
	Record.push_back(SR);
	PartRecord.push_back(SR);
	fprintf(tempf,"%lld,%s,%s,%d,%d,%d\n",SR.time,SR.src_ip.c_str(),SR.dst_ip.c_str(),SR.src_port,SR.dst_port,SR.size);
}
void GainRecord(FILE *f,FILE *res,vector<SubRecord> Record) {
	//fputs("TickName,PktNum,PktNumRate,AvgLength,IpEntropy,PortEntropy\n",f);//最终数据文件
	PortDstMap.clear();
	PortSrcMap.clear();
	IPDstMap.clear();
	IPSrcMap.clear();
	HIP.clear();
	int i,PktNum=0,PrePktNum=0,index=0;
	bool Pktflag=false;
	double PktNumRate=0,Avglength=0,IPSrcEntropy=0,IPDstEntropy=0,PortSrcEntropy=0,PortDstEntropy=0,HIPEntropy=0;
	long long begintime=0,tempT;//cout<<Record.size()<<" ";
	for(i=0,begintime=Record[0].time,PktNum=0; i<Record.size(); i++) {
		if(Record[i].time-begintime<delT) {
			PktNum++;
			Avglength+=Record[i].size;
			if(IPSrcMap.find(Record[i].src_ip)==IPSrcMap.end()) {
				IPSrcMap.insert(pair<string,int>(Record[i].src_ip,1));
			} else IPSrcMap[Record[i].src_ip]++;
			if(IPDstMap.find(Record[i].dst_ip)==IPDstMap.end()) {
				IPDstMap.insert(pair<string,int>(Record[i].dst_ip,1));
			} else IPDstMap[Record[i].dst_ip]++;
			if(PortSrcMap.find(Record[i].src_port)==PortSrcMap.end()) {
				PortSrcMap.insert(pair<int,int>(Record[i].src_port,1));
			} else PortSrcMap[Record[i].src_port]++;
			if(PortDstMap.find(Record[i].dst_port)==PortDstMap.end()) {
				PortDstMap.insert(pair<int,int>(Record[i].dst_port,1));
			} else PortDstMap[Record[i].dst_port]++;
		} else {
			//fprintf(gf,"%lld  %d\n",begintime,PktNum);
			//cout<<begintime<<"  "<<PktNum<<endl;
			if(PktNum>1) 
			{
				Pktflag=true;
				Avglength=Avglength/PktNum;
				if(PrePktNum!=0)PktNumRate=(double)(PrePktNum-PktNum)/PrePktNum;
				else PktNumRate=0;
				for(Iter=IPSrcMap.begin(); Iter!=IPSrcMap.end(); Iter++)	IPSrcEntropy+=((double)Iter->second/PktNum)*log((double)Iter->second/PktNum);
				for(Iter=IPDstMap.begin(); Iter!=IPDstMap.end(); Iter++)	IPDstEntropy+=((double)Iter->second/PktNum)*log((double)Iter->second/PktNum);
				for(IterInt=PortSrcMap.begin(); IterInt!=PortSrcMap.end(); IterInt++)	PortSrcEntropy+=((double)IterInt->second/PktNum)*log((double)IterInt->second/PktNum);
				for(IterInt=PortDstMap.begin(); IterInt!=PortDstMap.end(); IterInt++)	PortDstEntropy+=((double)IterInt->second/PktNum)*log((double)IterInt->second/PktNum);
				fprintf(f,"%lld-%lld,%d,%.2f,%.2f,",begintime,begintime+delT,PktNum,PktNumRate,Avglength);
				if(IPDstEntropy!=0)fprintf(f,"%.5f,",IPSrcEntropy/IPDstEntropy);
				else fprintf(f,"%.5f,",0);
				if(PortDstEntropy!=0)fprintf(f,"%.5f,",PortSrcEntropy/PortDstEntropy);
				else fprintf(f,"%.5f,",0); 
			//	fprintf(f,"%d\n",Tag); 
				fprintf(res,"%lld-%lld,%d,%.2f,%.2f,",begintime,begintime+delT,PktNum,PktNumRate,Avglength);
				if(IPDstEntropy!=0)fprintf(res,"%.5f,",IPSrcEntropy/IPDstEntropy);
				else fprintf(res,"%.5f,",0);
				if(PortDstEntropy!=0)fprintf(res,"%.5f,",PortSrcEntropy/PortDstEntropy);
				else fprintf(res,"%.5f,",0); 
//				fprintf(res,"%d\n",Tag); 
				if(JudgeAttack(begintime)){fprintf(res,"1\n");fprintf(f,"1\n");}
				else {fprintf(res,"0\n");fprintf(f,"0\n");}
				
				PrePktNum=PktNum;
			}
			if(i!=Record.size()-1)i--;
			begintime+=delT;
			PktNum=0;
			Avglength=0;
			PortDstMap.clear();
			PortSrcMap.clear();
			IPDstMap.clear();
			IPSrcMap.clear();
			IPSrcEntropy=0;
			IPDstEntropy=0;
			PortSrcEntropy=0;
			PortDstEntropy=0;
		}
	}
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
string GetString(FILE *f) {
	char c;
	string temps="";
	while((c=fgetc(f))!=EOF&&c!=' '&&c!='\n') {
		temps+=c;
	}
	return temps;
}
int StrToInt(string s) {
	int result=0;
	for(int i=0; i<s.size(); i++) {
		result+=(s[i]-'0')*pow(10,s.size()-1-i);
	}
	return result;
}
long long StrToLong(string s) {
	long long result=0;
	for(int i=0; i<s.size(); i++) {
		result+=(s[i]-'0')*pow(10,s.size()-1-i);
	}
	return result;
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

void CreateTable(string temps,FILE *SQLfp) {
	int i;
	string Str;
	for(i=17; i<temps.size(); i++)
		if(temps[i]!='.')Str+=temps[i];
		else break;
	fprintf(SQLfp,"DROP TABLE IF EXISTS `%s`;CREATE TABLE `%s`  (`TickName` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,",Str.c_str(),Str.c_str());
	fprintf(SQLfp,"`PktNum` int(20) NOT NULL,`PktNumRate` double(255, 2) NOT NULL,`AvgLength` double(255, 2) NOT NULL,");
	fprintf(SQLfp,"`IpEntropy` double(255, 5) NOT NULL,`PortEntropy` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,`Tag` int(255) NOT NULL) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci\n");
	fprintf(SQLfp,"ROW_FORMAT = Dynamic;SET FOREIGN_KEY_CHECKS = 1;\n");
	fprintf(SQLfp,"load data infile \"D:\\\\Mysql\\\\sql\\\\packet_in_result\\\\%s.txt\" into table %s fields terminated by ',' lines terminated by '\\n' ;",Str.c_str(),Str.c_str());
//	temps="D:\\Mysql\\sql脚本文件\\"+temps;
//	cout<<temps<<endl;
//	fprintf(SQLfp,"load data infile \"%s\" into table %s fields terminated by ',' lines terminated by '\\n' ;",temps.c_str(),Str.c_str());
}

void ChangeTime(long long &time)
{
	int temps=time%100,tempm;
	temps+=delT;
	if(temps>=60)
	{
		temps-=60;
		time/=100;
		tempm=time%100;
		tempm++;
		if(tempm>=60)
		{
			tempm=0;
			time/=100;
			time++;
			time*=10000;
			time+=temps;
		}
		else
		{
			time++;
			time*=100;
			time+=temps;
		}
	}
	else
	{
		time+=delT;
	}
}
