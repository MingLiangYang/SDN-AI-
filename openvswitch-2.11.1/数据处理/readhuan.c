#include <stdio.h>  
#include <unistd.h>  
#include <stdlib.h>  
#include <string.h>  
#include <errno.h>
#include <time.h>  
//#include <linux/autoconf.h>//内核编译的配置信息  
#include <sys/klog.h>  
#include <sys/stat.h>  
//#define __LOG_BUF_LEN   (1 << CONFIG_LOG_BUF_SHIFT)//在2.6.28内核中为默认1<<17，这才是真正dmesg buffer的大小，网上其他都扯淡。  
#define __LOG_BUF_LEN   (262144)//在2.6.28内核中为默认1<<17，这才是真正dmesg buffer的大小，网上其他都扯淡。  
#define __LOG_PATH      "home/gary/my.txt"  
#define LOG_SLEEP(x)    (sleep(x))  
#define __LOG_SIZE      10485760//大于10M时删除文件  
#define BUF_SIZE       512 
  
  
long check_log_size(void)  
{  
    struct stat f_stat;  
    if( stat( __LOG_PATH, &f_stat ) == -1 )  
    {  
        return -1;  
    }  
    return (long)f_stat.st_size;  
}  
  
int main(int argc, char *argv[])  
{  
    char buf[__LOG_BUF_LEN]={0,};  
    char tmpbuf[BUF_SIZE]={0,};  
    int ret = 0;  
    FILE *fp =NULL;  
    struct tm *ptr;  
    time_t lt;  
    //daemon(0,0);//进入守护模式  
    while(1)  
    {  
	
        LOG_SLEEP(2);//sleep 10 秒  
        fp = fopen("my.txt","a");//追加打开  
        if(NULL == fp)  
        {  
            printf("creat file faild !\n");  
            continue;  
        }  
        ret = klogctl(4,buf,__LOG_BUF_LEN);//获得dmesg信息,该函数需要超级用户权限运行  
            if(ret>=__LOG_BUF_LEN-1){
            char *error="too big!!";
            fwrite(error,strlen(error),1,fp);   
        }
        if(0 >= ret){  
            perror("klogctl ");  
            fclose(fp);  
            continue;  
        }    
        fwrite(buf,strlen(buf),1,fp);  
        fflush(fp);  
        fclose(fp);  
        if(__LOG_SIZE < check_log_size())  
        {  
            unlink(__LOG_PATH);//删除该文件  
        }  
        memset(tmpbuf,0,BUF_SIZE);  
        memset(buf,0,__LOG_BUF_LEN);  
    }  
    return 0;  
} 
