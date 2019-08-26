
DROP TABLE IF EXISTS `Packet_in`;
CREATE TABLE `Packet_in`  (
  `index` int(255) NOT NULL ,
  `PktNum` int(20) NOT NULL,
  `PktNumRate` double(255, 2) NOT NULL,
  `AvgLength` double(255, 2) NOT NULL,
  `IpEntropy` double(255, 5) NOT NULL,
  `PortEntropy` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `Tag` int(255) NOT NULL ,
  PRIMARY KEY (`index`) USING BTREE
);

SET FOREIGN_KEY_CHECKS = 1;
load data infile "D:\\Mysql\\sql\\AI_result\\packet_in_vector.txt" into table Packet_in fields terminated by ',' lines terminated by '\n' ;

DROP TABLE IF EXISTS `ip_info_2`;
CREATE TABLE `ip_info_2`  (
  `index` int(20) NULL DEFAULT NULL,
  `Switchid` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Src_ip` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Dst_ip` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Etropy` double(50, 5) NULL DEFAULT NULL,
  `Tag` int(20) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

DROP TABLE IF EXISTS `switch_port`;
CREATE TABLE `switch_port`  (
  `index` int(20) NULL DEFAULT NULL,
  `Port` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `PacketReceivedAll` int(50) NULL DEFAULT NULL,
  `PacketReceivedSuccess` int(50) NULL DEFAULT NULL,
  `PacketsTransmittedAll` int(50) NULL DEFAULT NULL,
  `PacketTransmittedSuccess` int(50) NULL DEFAULT NULL,
  `ByteReceivedAll` int(50) NULL DEFAULT NULL,
  `ByteTransmittedAll` int(50) NULL DEFAULT NULL,
  `Tag` int(20) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

DROP TABLE IF EXISTS `switch`;
CREATE TABLE `switch`  (
  `index` int(20) NULL DEFAULT NULL,
  `Switch` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `PacketReceivedAll` int(50) NULL DEFAULT NULL,
  `PacketReceivedSuccess` int(50) NULL DEFAULT NULL,
  `PacketsTransmittedAll` int(50) NULL DEFAULT NULL,
  `PacketTransmittedSuccess` int(50) NULL DEFAULT NULL,
  `ByteReceivedAll` int(50) NULL DEFAULT NULL,
  `ByteTransmittedAll` int(50) NULL DEFAULT NULL,
  `CPUi` double(255, 5) NULL DEFAULT NULL,
  `Memory` double(255, 5) NULL DEFAULT NULL,
  `Tag` int(20) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

load data infile "D:\\Mysql\\sql\\AI_result\\ovs_port.txt" into table switch_port fields terminated by ',' lines terminated by '\n' ;

load data infile "D:\\Mysql\\sql\\AI_result\\ovs_switch.txt" into table switch fields terminated by ',' lines terminated by '\n' ;

load data infile "D:\\Mysql\\sql\\AI_result\\ovs_ip_info.txt" into table ip_info_2 fields terminated by ',' lines terminated by '\n' ;

