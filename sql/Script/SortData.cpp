#include<iostream>
#include<vector>
#include<algorithm>
using namespace std;
struct Record
{
	char time[100];
	char temp1[100];
	char temp2[100];
	char temp3[100];
	char temp4[100];
	char temp5[100];
	char temp6[100];
	char temp7[100];
};
vector<string> Result;
bool com(Record a,Record b)
{
	return a.time<b.time;
}
int main()
{
	FILE *f1,*fp;
	string temp;
	if((f1=fopen("D:\\Mysql\\sql\\packet_in_result\\packet_in_vector.txt","r"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	if((fp=fopen("D:\\Mysql\\sql\\packet_in_result\\packet_in_vector(sort).txt","w"))==NULL)
	{
		printf("The file can not be open!");
    	exit(1);//结束程序的执行
	} 
	while(!feof(f1))
	{
		temp.resize(100);
		fscanf(f1,"%s\n",&temp[0]);
		//fprintf(fp,"%s\n",temp.c_str());
		//fscanf(f1,"%s,%s,%s,%s,%s,%s,%s,%s\n",R.time,R.temp1,R.temp2,R.temp3,R.temp4,R.temp5,R.temp6,R.temp7);
		Result.push_back(temp);
	}
//	cout<<Result.size()<<endl;
//		cout<<fprintf(fp,"hhh");
	sort(Result.begin(),Result.end());
	for(int i=0;i<Result.size();i++)
	{
	//	cout<<Result[i]<<endl;
		fprintf(fp,"%s\n",Result[i].c_str());
	}
	return 0;
} 
