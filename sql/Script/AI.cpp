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
int main() {
	int index=0,d1,d2;
	float f1,f2,f3,f4;
	FILE *Numfp;
	vector<int> D1,D2;
	vector<float> F1,F2,F3,F4;
	int AttackRecord,INDEXNUMBER;
	FILE *fp,*AIfp;
	if((fp=fopen("packet_in_result\\packet_in_vector(sort).txt","r"))==NULL) { 
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
	if((Numfp=fopen("packet_in_result\\Number.txt","r"))==NULL) { 
		printf("The file can not be open!");
		exit(1);//结束程序的执行
	}
	if((AIfp=fopen("packet_in_result\\AIDatas.txt","w"))==NULL) { 
		printf("The file AIData can not be open!");
		exit(1);//结束程序的执行
	}
	fscanf(Numfp,"%d %d",&AttackRecord,&INDEXNUMBER);
	while(!feof(fp))
	{
		GetString(fp);
		fscanf(fp,"%d,%f,%f,%f,%f,%d\n",&d1,&f1,&f2,&f3,&f4,&d2);
		fprintf(AIfp,"%d,%d,%f,%f,%f,%f,%d\n",++index,d1,f1,f2,f3,f4,d2);
	}
	return 0;
}
