#include<iostream>
#include<vector>
#include<math.h>
#include<map>
using namespace std;
string GetString(FILE *f) {
	char c;
	string temps="",res="";
	while((c=fgetc(f))!=EOF&&c!=' '&&c!='\n'&&c!=',') {
		temps+=c;
	}
//	for(int i=1;i<temps.size()-1;i++) res+=temps[i];
	return temps;
}
void HandleIP_info();
void HandlePort();
void HandleSwitch();
int main() {
	int index=0,d1,d2;
	float f1,f2,f3,f4;
	FILE *Numfp;
	vector<int> D1,D2;
	vector<float> F1,F2,F3,F4;
	int AttackRecord,INDEXNUMBER;
	FILE *fp,*AIfp;//fp1对应于存储端口收到转发数据包的信息，fp2对应数据包源端口、目的端口的信息。以下代码中标识“1”和标识“2”代表的东西也如此
	if((fp=fopen("packet_in_result\\packet_in_vector.txt","r"))==NULL) { //packet_in是处理之前的文件名
		printf("The file packet_in_vector.txt can not be open!");
		exit(1);//结束程序的执行
	}
	if((AIfp=fopen("AI_result\\packet_in_vector.txt","w"))==NULL) { //packet_in是处理之前的文件名
		printf("The file AIData can not be open!");
		exit(1);//结束程序的执行
	}
	while(!feof(fp))
	{
		GetString(fp);
		fscanf(fp,"%d,%f,%f,%f,%f,%d\n",&d1,&f1,&f2,&f3,&f4,&d2);
		fprintf(AIfp,"%d,%d,%f,%f,%f,%f,%d\n",++index,d1,f1,f2,f3,f4,d2);
	}
	HandleIP_info();
	HandlePort();
	HandleSwitch();
	return 0;
}
void HandleIP_info()
{
	FILE *fp,*AIfp;
	string s1,s2,s3;
	float f1;
	int t1,index=0;
	if((fp=fopen("ovs_result\\ovs_ip_info.txt","r"))==NULL) { //packet_in是处理之前的文件名
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
	if((AIfp=fopen("AI_result\\ovs_ip_info.txt","w"))==NULL) { //packet_in是处理之前的文件名
		printf("The file AIData can not be open!");
		exit(1);//结束程序的执行
	}
	while(!feof(fp))
	{
		GetString(fp);s1=GetString(fp);s2=GetString(fp);s3=GetString(fp);
		fscanf(fp,"%f,%d\n",&f1,&t1);
	//	cout<<s1<<"  "<<f1<<"  "<<t1<<endl;
		fprintf(AIfp,"%d,%s,%s,%s,%f,%d\n",++index,s1.c_str(),s2.c_str(),s3.c_str(),f1,t1);
	}
}
void HandlePort()
{
	FILE *fp,*AIfp;
	string s1,s2,s3;
	float f1,f2;
	int t1,t2,t3,t4,t5,t6,t7,t8,index=0;
	if((fp=fopen("ovs_result\\ovs_port.txt","r"))==NULL) { //packet_in是处理之前的文件名
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
	if((AIfp=fopen("AI_result\\ovs_port.txt","w"))==NULL) { //packet_in是处理之前的文件名
		printf("The file AIData can not be open!");
		exit(1);//结束程序的执行
	}
	while(!feof(fp))
	{
		GetString(fp);s1=GetString(fp);
		fscanf(fp,"%d,%d,%d,%d,%d,%d,%d\n",&t1,&t2,&t3,&t4,&t5,&t6,&t7);
		//cout<<s1<<"  "<<t1<<"  "<<t4<<endl;
		fprintf(AIfp,"%d,%s,%d,%d,%d,%d,%d,%d,%d\n",++index,s1.c_str(),t1,t2,t3,t4,t5,t6,t7);
	}
}
void HandleSwitch()
{
	FILE *fp,*AIfp;
	string s1,s2,s3;
	float f1,f2;
	int t1,t2,t3,t4,t5,t6,t7,index=0;
	if((fp=fopen("ovs_result\\ovs_switch.txt","r"))==NULL) { //packet_in是处理之前的文件名
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
	if((AIfp=fopen("AI_result\\ovs_switch.txt","w"))==NULL) { //packet_in是处理之前的文件名
		printf("The file AIData can not be open!");
		exit(1);//结束程序的执行
	}
	while(!feof(fp))
	{
		GetString(fp);s1=GetString(fp);
		fscanf(fp,"%d,%d,%d,%d,%d,%d,%f,%f,%d\n",&t1,&t2,&t3,&t4,&t5,&t6,&f1,&f2,&t7);
		//cout<<s1<<"  "<<f1<<"  "<<t1<<endl;
		fprintf(AIfp,"%d,%s,%d,%d,%d,%d,%d,%d,%f,%f,%d\n",++index,s1.c_str(),t1,t2,t3,t4,t5,t6,f1,f2,t7);
	}
}
