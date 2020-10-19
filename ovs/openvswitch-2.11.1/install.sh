#!/bin/bash

#此脚本用于一在电脑上一键安装ovs，经过测试ubuntu18有效。
#脚本会检测ovs版本是否正确，如果不正确会切换内核版本，切换内核会重启机器，并且有可能会使得机器卡死
#卡死的原因一般都是图形界面驱动的问题，用ssh连上还是能流畅运行。重启机器之后请再次执行次脚本，执行完毕后使用dmesg命令，
#如果看到这样的输出:
#[  604.878727] openvswitch: Open vSwitch switching datapath upcall delay mingliang!!! 8 8 16 2.11.1
#[  604.878828] openvswitch: LISP tunneling driver
#[  604.878829] openvswitch: STT tunneling driver
#以及存在文件/var/log/gary.log，那就说明你的ovs已经安装成功了


set -e

set -x
#判断内核版本是否正确，如果不正确那么切换内核
kernel_version=$(uname -r)
right_version="4.18.0-25"
if [ ${kernel_version:0:9} != $right_version ]
then
        #切换内核
        echo "Error kernel version, now change your kernel to right version!"
        apt-get install -y linux-headers-${right_version}-generic linux-image-${right_version}-generic
        #寻找刚刚安装内核在grub中的启动位置
        grub_menu="menuentry "
        #不知道为什么number设置为0或者-1的时候会出错，可能是((number++))的时候number不能等于0，好像是因为命名的问题
        boot_index=-1
        set +x
        while read line
        do
            if [[ $line =~ $grub_menu ]]
                then
                    if [[ $line =~ $right_version ]]
                    then
                        break
                    fi
                    #((boot_index++))好像是一种不正确的写法，但是却能自增，如果不使用set +e屏蔽错误继续执行就会导致错误退出
		            set +e
                    ((boot_index++))
		            set -e
                fi
        done < /boot/grub/grub.cfg
        echo "boot_index=${boot_index}"
        #替换/etc/default/grub文件中的默认内核启动项
        grub_defaut=" "
        while read line1
        do
                if [[ $line1 =~ "GRUB_DE" ]]
                then
                        grub_defaut=$line1
                        break
                fi
        done < /etc/default/grub
        set -x
        sed -i "s/${grub_defaut}/GRUB_DEFAULT=\"1>${boot_index}\"/g" /etc/default/grub
        update-grub
        echo "Your computer will reboot!!!!!!!!!!!"
        sleep 7
	unset kernel_version 
	unset right_sersion
        reboot
fi

set +e
#删除OVS，防止系统可能预装了OVS
rm /var/log/gary.log
killall ovsdb-server
killall ovs-vswitchd
apt-get remove openvswitch-common openvswitch-datapath-dkms openvswitch-controller openvswitch-pki openvswitch-switch
ovs-dpctl del-dp ovs-system
modprobe -r openvswitch 
set -e

#安装必要的软件
apt-get install gcc make autoconf libtool libelf-dev

echo "local4.*    /var/log/gary.log" >>/etc/rsyslog.conf
/etc/init.d/rsyslog restart

for dir in /lib/modules/*/kernel/net/openvswitch/
do
    rm -rf $dir
done

chmod -R 777 ./
chmod +x ./boot.sh

./boot.sh
./configure --with-linux=/lib/modules/$(uname -r)/build
make -j4
make install
make modules_install
modprobe openvswitch

set +e
mkdir -p /usr/local/etc/openvswitch
ovsdb-tool create /usr/local/etc/openvswitch/conf.db vswitchd/vswitch.ovsschema
mkdir -p /usr/local/var/run/openvswitch
ovsdb-server --remote=punix:/usr/local/var/run/openvswitch/db.sock \
    --remote=db:Open_vSwitch,Open_vSwitch,manager_options \
    --private-key=db:Open_vSwitch,SSL,private_key \
    --certificate=db:Open_vSwitch,SSL,certificate \
    --bootstrap-ca-cert=db:Open_vSwitch,SSL,ca_cert \
    --pidfile --detach
set -e

ovs-vsctl --no-wait init
ovs-vswitchd --pidfile --detach --log-file
