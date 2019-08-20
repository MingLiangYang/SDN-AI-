概述：  

    是本SDN-AI项目的Controller部分。  
    主要功能是：  
        1 在ovs连接时下放默认流表  
        2 接收交换机上送的IP Packet-In，并根据指定路径给交换机下发Packet-Out和双向流表  
        3 收到IP Packet-In后根据AI模块的参数需求进行解析，然后输出到文件中
        4 根据openflow协议收集ovs的端口和流表统计
        
组织架构：  

	该controller使用的是karaf的架构。
	/features 中有该项目依赖的其他feature
    	/impl 中有Controller的具体功能的实现代码
	
详细描述：

    defender/PacketHandler：
    收到PacketIn后解析，如果是IP类型的就将PacketIn写到文件中
    ovsstats/statsProvider：
    创建Timer，每隔3s 使用readTransaction读取DS的Nodes信息，然后输出Nodes/Node的端口和流统计信息
    packetHandler/PacketInHandler：
    收到PacketIn后解析，如果是IP PacketIn，调用inventoryReader.getNodeConnector(ingress.getValue().firstIdentifierOf(Node.class), 
    desMac);inventoryReader使用readTransaction读取DS的这个Node的信息，遍历Node所有端口的记录地址，找到desMac后返回端口。根据端口的值，如果端口
    存在则sendPacketOut addBidirectionalMacToMacFlows，否则 defaultAction
	
    

 
    
    
