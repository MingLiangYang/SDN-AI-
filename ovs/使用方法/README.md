如何编译安装该OVS，并使用其记录流经该OVS的特征数据（详细特征数据参考技术文档）？

1.首先，因为OVS包括内核态和用户态，因此特征数据有些是在内核态下，另一些是在用户态下。
这导致了内核态特征数据和用户态特征数据是使用不同方法来取。

2.内核态的数据会输出到/var/log/kern.log中。

3.用户态的数据使用rsysylog来记录，需要手动在/etc/rsyslog.conf配置文件中添加如下语句(保存文件位置自行修改)：
```
local4.*	/home/gary/gary.log
```
然后到相应目录创建上述命令中的log文件。并修改log文件以及所在目录权限，否则无法写入。
然后重启日志系统，在shell中运行如下命令
```
/etc/init.d/rsyslog restart #重启日志系统
```

4.接下来运行如下编译安装命令
编译安装命令：
```
./boot.sh
./configure --with-linux=/lib/modules/$(uname -r)/build
make -j4
make install
make modules_install
modprobe openvswitch
lsmod | grep openvswitch#可以看到ovs相关的内核模块输出
mkdir -p /usr/local/etc/openvswitch
ovsdb-tool create /usr/local/etc/openvswitch/conf.db vswitchd/vswitch.ovsschema #可能报错说已经存在，这种情况不用管，继续
mkdir -p /usr/local/var/run/openvswitch
ovsdb-server --remote=punix:/usr/local/var/run/openvswitch/db.sock \
    --remote=db:Open_vSwitch,Open_vSwitch,manager_options \
    --private-key=db:Open_vSwitch,SSL,private_key \
    --certificate=db:Open_vSwitch,SSL,certificate \
    --bootstrap-ca-cert=db:Open_vSwitch,SSL,ca_cert \
    --pidfile --detach
ovs-vsctl --no-wait init
ovs-vswitchd --pidfile --detach --log-file
ps -ea | grep ovs#有相关ovs进程输出
```

5.删除OVS，运行如下命令(如果需要删除的话)：
```
sudo killall ovsdb-server
sudo killall ovs-vswitchd
sudo apt-get remove openvswitch-common openvswitch-datapath-dkms openvswitch-controller openvswitch-pki openvswitch-switch
ovs-dpctl del-dp ovs-system
modprobe -r openvswitch 
rmmod openvswitch
```

6.最后获得实验数据kern.log和gary.log分别保存着内核态数据和用户态数据，
  使用当前文件夹下的data_process.py对数据进行处理，生成文件
  ```datapath_Gary,datapath_upcall,user_table_time,user_upcall和userspace```

  具体数据含义入下：
  ```
	datapath_Gary文件：
		timep、 ip  、ip、port、port、 
		hit_kernalFlow_packNum、find_kernalFlow_msec、ovs_flow_cmd_set_num、
		ovs_flow_cmd_get_num、ovs_flow_cmd_del_num、ovs_execute_actions_num、
		hit_catch_num、cmd_fail_times;
			
	user_upcall文件：
		timep、upcall_delay
			
	datapath_upcall文件：
		timep、upcall_num、upcall_length

	user_table_time文件：
		timep、user_table_time
			
	userspace文件:
		timep、recv_controller、send_controller、udpif_upcall_handler、hit_user_table_count、user_table_count、main_times。
```


  好了，现在可以把数据交给叫我金处理了！


！！！！！！！！
注意事项：该版本支持的linux内核为4.18，内核过高会出现许多问题，请使用4.18版本的内核！
