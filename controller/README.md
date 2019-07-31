概述：
    是本SDN-AI项目的Controller部分。
    主要功能是：
        1 在ovs连接时下放默认流表
        2 接收交换机上送的IP Packet-In，并根据指定路径给交换机下发Packet-Out和双向流表
        3 收到IP Packet-In后根据AI模块的参数需求进行解析，然后输出到文件中
        
组织架构：
    controller文件夹中是整个基于Maven命令生成的Controller的文件架构，该Controller基于karaf
    features中有Controller的依赖feature
    impl是Controller的具体功能的实现
    impl/pom.xml中有Controller的依赖文件
    impl/src/main/java/org/jbh/controller/impl/中有三个模块，分别对应上述三个主要功能
    impl/src/main/resources/org/opendaylight/blueprint/impl-blueprint.xml是blueprint文件
    
    
