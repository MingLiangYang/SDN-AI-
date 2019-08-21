#include<stdlib.h>
#include <unistd.h>
int main()
{
	int i=0;
	int tag=1;
	while(i++<40000){
		system("date +%s >> cpu_memory_data");
		system("ps -aux|grep revalidator|awk '{print $3,$4}' >> cpu_memory_data");
		system("ps -aux|grep ovsdb-server|awk '{print $3,$4}' >> cpu_memory_data");
		system("ps -aux|grep ovs-vswitchd|awk '{print $3,$4}' >> cpu_memory_data");
		system("ps -aux|grep ksoftirqd|awk '{print $3,$4}' >> cpu_memory_data");
		sleep(1);
	}
	return 0;
}
