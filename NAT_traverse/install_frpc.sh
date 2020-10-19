#!/bin/bash
#执行改脚本就可以实现当前主机被穿透，其他人可以通过ssh -oPort=port user_name@150.158.157.122
#控制你的电脑，其中port是下面代码中的remote_port的变量值。user_name是被穿透主机的用户名。
#如果想要向被穿透主机发送文件可以通过命令scp -r -P port source_file user_name@150.158.157.122:des_dir,
#其中port和user_name跟上个命令一样，source_file和des_dir是要传送的文件（或者文件夹）和存放在被穿透主机的路径
set -e
set -x
session_name=jbh
remote_port=6000
if [ ! -e "frp_*tar*" ]
then
	apt-get install wget
	wget https://github.com/fatedier/frp/releases/download/v0.34.1/frp_0.34.1_linux_amd64.tar.gz
fi
if [ ! -e "frp_*amd64" ]
then
	tar zxf frp_0.34.1_linux_amd64.tar.gz
fi
cd frp_0*amd64
sed -i "s/server_addr = 127.0.0.1/server_addr = 150.158.157.122/g" ./frpc.ini
sed -i "s/server_port = 7000/server_port = 80/g" ./frpc.ini
sed -i "s/remote_port = 6000 /remote_port = ${remote_port} /g" ./frpc.ini
sed -i "s/ssh/${session_name}/g" ./frpc.ini
./frpc -c ./frpc.ini