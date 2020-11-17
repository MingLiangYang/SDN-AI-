# Internet2 实验重放步骤

### 第一次实验前配置 —— 0.重启三台服务器，执行配置脚本

第一次实验前需要重启三台服务器，并在服务器上运行配置脚本。之后做实验只需要按照本文下面的**<u>实验步骤</u>**进行。

##### 0. 重启服务器，执行配置脚本：

打开一个terminal终端（！这个终端不可关闭）

```shell
cd topology
sudo su
./init_tap.sh
./set_host3.sh
./run_ovs3.sh
./set_arp.sh
cd simple-switch
./netmod.sh
```

PS. 重启之后的第一次实验经过测试是可行的，如果在之后的实验环境中出现无法解决的拓扑故障，可以使用**<u>第一次实验配置</u>**的方法解决——即重启并执行配置脚本。

### 实验步骤 —— 1.启动虚拟机，2.运行控制器，3.连通性测试，4.运行重放脚本，5.收集数据脚本。6.实验结束后关闭虚拟机和控制器。

##### 1. 启动虚拟机

分别在左3、左5、右2的服务器上执行以下命令：

```shell
cd topology
./startvm.sh
```

##### 2. 运行控制器

在右1的服务器上执行以下命令：

```shell
cd /home/zju/Desktop
sudo su
./karaf.sh
```

等待2-5分钟，等交换机全部连接到控制器。拓扑连接成功与否可以打开浏览器输入localhost:8181/index.html，默认账号/密码：admin/admin，点击topology按钮查看。

##### 3. ping test（连通性测试）

在左3、左5、右2任意一台服务器的一个主机namespace中，ping其他主机。

以右2为例，用h2 ping 其他机器：

```shell
sudo ip netns exec h2 ping 10.0.0.1 -c 2
sudo ip netns exec h2 ping 10.0.0.3 -c 2
sudo ip netns exec h2 ping 10.0.0.4 -c 2
sudo ip netns exec h2 ping 10.0.0.5 -c 2
sudo ip netns exec h2 ping 10.0.0.6 -c 2
sudo ip netns exec h2 ping 10.0.0.7 -c 2
sudo ip netns exec h2 ping 10.0.0.8 -c 2
sudo ip netns exec h2 ping 10.0.0.9 -c 2
sudo ip netns exec h2 ping 10.0.0.10 -c 2
sudo ip netns exec h2 ping 10.0.0.11 -c 2
```

##### 4. pacp文件重放 

分别在左3、左5、右2三台服务器的桌面上运行replay.sh

```shell
cd Desktop
sudo ./replay.sh
```

##### 5. 运行收集数据脚本

分别在五个域的边缘交换机s1、s7、s9、s10、s12还有服务器的边缘交换机s15，一共六个虚拟机里运行数据收集脚本

​	s1和s7在右2；s9和s10在左3；s12和s15在左5。

```shell
cd Desktop
sudo ./get_flow_s8.sh
```

如果是远程情况，可以使用如下命令

```shell
cd Desktop
sudo su
nohup ./get_flow_s8.sh &
```



##### 6.  实验结束，关闭虚拟机、控制器

把左3、左5、右2一共17个虚拟机正常关闭了；在右1的控制器终端里输入ctrl+d关闭终端。



### PS 实验步骤 2、4 、5和 6 中的关闭控制器，需要在之江人员远程协助一下。帮助运行相应脚本即可。
