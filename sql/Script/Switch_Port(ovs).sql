DROP TABLE IF EXISTS `switch_port`;
CREATE TABLE `switch_port`  (
  `Time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Port` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `PacketReceivedAll` int(50) NULL DEFAULT NULL,
  `PacketReceivedSuccess` int(50) NULL DEFAULT NULL,
  `PacketsTransmittedAll` int(50) NULL DEFAULT NULL,
  `PacketTransmittedSuccess` int(50) NULL DEFAULT NULL,
  `ByteReceivedAll` int(50) NULL DEFAULT NULL,
  `ByteTransmittedAll` int(50) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

DROP TABLE IF EXISTS `switch`;
CREATE TABLE `switch`  (
  `Time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Switch` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `PacketReceivedAll` int(50) NULL DEFAULT NULL,
  `PacketReceivedSuccess` int(50) NULL DEFAULT NULL,
  `PacketsTransmittedAll` int(50) NULL DEFAULT NULL,
  `PacketTransmittedSuccess` int(50) NULL DEFAULT NULL,
  `ByteReceivedAll` int(50) NULL DEFAULT NULL,
  `ByteTransmittedAll` int(50) NULL DEFAULT NULL,
  `CPUi` double(255, 5) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

DROP TABLE IF EXISTS `ip_info`;
CREATE TABLE `ip_info`  (
`Time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,

  `Switch` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Src_IP` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Dst_IP` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Inport` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Outport` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `Src_IPEntroy` double(255, 5) NULL DEFAULT NULL,
  `Dst_IPEntroy` double(255, 5) NULL DEFAULT NULL,
  `PacketCount` int(50) NULL DEFAULT NULL,
  `ByteCount` int(50) NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


load data infile "D:\\Mysql\\sql\\ovs_result\\ovs_port.txt" into table switch_port fields terminated by ',' lines terminated by '\n' ;

load data infile "D:\\Mysql\\sql\\ovs_result\\ovs_switch.txt" into table switch fields terminated by ',' lines terminated by '\r\n' ;

load data infile "D:\\Mysql\\sql\\ovs_result\\ovs_ip_info.txt" into table ip_info fields terminated by ',' lines terminated by '\n' ;
