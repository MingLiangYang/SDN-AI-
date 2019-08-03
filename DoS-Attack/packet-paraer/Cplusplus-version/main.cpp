#include <time.h>
#include "getpkt.h"

u_int64 n;
u_int64 k;
u_int64 m;

struct fiveTuple_t pktTuplebuf[38000001];						//store 5-tuple of pkt in memory

int main()
{
	char fname[30]={0};
	for(int i=0;i<14;++i){
		sprintf(fname,"use%d.pcap",i);

	extracter a;
	a.extract(fname,pktTuplebuf,1000000, i);
	memset(pktTuplebuf, 0, sizeof(pktTuplebuf));
	printf("%d\n",i);
}

	return 0;
}
