用户表：
CREATE TABLE `user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(16) NOT NULL,
  `comment` varchar(255) NOT NULL DEFAULT '' COMMENT '个人介绍',
  `isvip` tinyint(3) unsigned NOT NULL COMMENT '1是vip,0不是',
  `usedspace` mediumint(11) unsigned NOT NULL DEFAULT '0' COMMENT '用户已使用空间(MB);最大支持上限约8T',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqueName` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=949 DEFAULT CHARSET=utf8

文件表：
CREATE TABLE `file` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `filename` varchar(255) NOT NULL COMMENT '文件名',
  `filepath` varchar(600) NOT NULL COMMENT '文件路径',
  `filesize` float unsigned NOT NULL COMMENT '文件MB大小',
  `createtime` date DEFAULT NULL COMMENT '创建日期',
  `canshare` int(2) NOT NULL COMMENT '0表示私有 1表示共享',
  `user_id` int(11) unsigned NOT NULL,
  `MD5` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `file_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1293 DEFAULT CHARSET=utf8