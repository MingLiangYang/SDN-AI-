概述：  

    本Controller是项目里的SDN控制器模块，主要功能有：
    1 在ovs连接控制器后下方默认流表
    2 收到ovs的IP Packet-In后，下放Packet-Out并给该ovs下发IP流的双向流表
    3 记录收到的IP Packet-In，将记录写到文件内供AI模块使用
    
组织架构：  

    本Controlle通过maven命令生成的karaf-0.8.3（氧SR3）的代码架构。
    features中有该项目依赖的其他feature，impl中有实现代码的调用依赖
    impl\src\main\java\org\jbh\flowcontroller\impl中是三个功能模块的实现代码
    impl\src\main\resources\org\opendaylight\blueprint中有blueprint文件。
