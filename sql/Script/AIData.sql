DROP TABLE IF EXISTS `AIData`;
CREATE TABLE `AIData`  (
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
load data infile "D:\\Mysql\\sql脚本文件\\packet_in_result\\AIDatas.txt" into table AIData fields terminated by ',' lines terminated by '\n' ;
