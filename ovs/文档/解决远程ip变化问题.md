### 列出所有的虚拟机

```
vboxmanage list vms
```

效果如图：

![image-20201029173419850](C:\Users\admin\Desktop\ovs\SDN-AI-\ovs\img\image-20201029173419850.png)



### 列出特定虚拟机的详细信息

```
vboxmanage showvminfo vm_name -details
```

`vm_name`为虚拟机名字。



### 查看宿主机ARP缓存

```
arp -a
```

效果如下图所示，可以查看相应mac地址的IP。

![image-20201029173000260](C:\Users\admin\Desktop\ovs\SDN-AI-\ovs\img\image-20201029173000260.png)