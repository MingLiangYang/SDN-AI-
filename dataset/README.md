数据集来源：https://www.unb.ca/cic/datasets/ids-2017.html

**`monday`为正常流量数据**

**`wednesday`含有正常及攻击数据**

以下时间描述为当地时间（**UTC-3**），从 pcap 包获取数据时注意时间转换(+11h)

#### Wednesday, July 5, 2017

##### DoS / DDoS

|     Type     |  Time(UTC-3)  |  Time(UTC+8)  |            Attacker             |               Victim                |
| :----------: | :-----------: | :-----------: | :-----------------------------: | :---------------------------------: |
|  slowloris   | 9:47 – 10:10  | 20:47 - 21:10 | 205.174.165.73<br>(172.16.0.1)  | 205.174.165.68 <br>(192.168.10.50)  |
| Slowhttptest | 10:14 – 10:35 | 21:14 - 21:35 | 205.174.165.73<br/>(172.16.0.1) | 205.174.165.68 <br/>(192.168.10.50) |
|     Hulk     | 10:43 – 11:00 | 21:43 - 22:00 | 205.174.165.73<br/>(172.16.0.1) | 205.174.165.68<br/> (192.168.10.50) |
|  GoldenEye   | 11:10 – 11:23 | 22:10 - 22:23 | 205.174.165.73<br/>(172.16.0.1) | 205.174.165.68 <br/>(192.168.10.50) |

#### Thursday, July 6, 2017

##### DoS / DDoS

|    Type    |  Time(UTC-3)  |  Time(UTC+8)  |             Attacker             |               Victim                |
| :--------: | :-----------: | :-----------: | :------------------------------: | :---------------------------------: |
| Heartbleed | 15:12 - 15:32 | 02:12 - 02:32 | 205.174.165.73<br/>(172.16.0.11) | 205.174.165.66 <br/>(192.168.10.51) |

#### 使用工具及命令：

##### pcap包修改：`tcprewrite`,`wireshark`

- 映射ip：

  ```shell
  tcprewrite -i input.pcap -o output.pcap --srcipmap=192.168.10.51:192.168.11.21 --dstipmap=192.168.10.51:192.168.11.21
  ```

- 映射MAC：

  ```shell
  tcprewrite -i res1.pcap -o res.pcap --enet-subsmac=00:c1:b1:14:eb:31,08:00:27:85:5c:b0
  ```

  

##### 分割数据包(获取部分数据包或数据包太大而不方便处理时可使用)：

- 基于包的数量分割：

```shell
editcap -c <packets per file> input.pcap output.pcap
editcap -c 100000 input.pcap output.pcap
```



- 基于时间分割：

```shell
editcap -A <start time> -B <stop time> input.pcap output.pcap
editcap -A "2017-07-05 20:47:00" -B "2017-07-05 21:10:00" input.pcap output.pcap
```



- 合并数据包：默认基于每个frame的`timestamp`进行合并，使用`-a`则忽略`timestamp`，直接将每个`pcap`包拷贝到`output`文件

```
mergecap -w output.pcap input1.pcap input2.pcap
```

- 重放`pcap`包：`tcpreplay`

```shell
tcpreplay -i eno2 replay.pcap
```

- 修改MTU：

  ```shell
  ifconfig eth1 mtu 9000 up
  ```

  



