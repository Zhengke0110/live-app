--- 创建一百张分表的脚本
DELIMITER $$
CREATE PROCEDURE live_user.create_t_user_100 () BEGIN DECLARE i INT; DECLARE table_name VARCHAR (30); DECLARE table_pre VARCHAR (30); DECLARE sql_text VARCHAR (3000); DECLARE table_body VARCHAR (2000);
SET i=0;
SET table_name='';
SET sql_text='';
SET table_body='(
  user_id bigint NOT NULL DEFAULT -1 COMMENT \'用户id
\',
  nick_name varchar(35)  DEFAULT NULL COMMENT \'昵称
\',
  avatar varchar(255)  DEFAULT NULL COMMENT \'头像
\',
  true_name varchar(20)  DEFAULT NULL COMMENT \'真实姓名
\',
  sex tinyint(1) DEFAULT NULL COMMENT \'性别 0男
，1女
\',
  born_date datetime DEFAULT NULL COMMENT \'出生时间
\',
  work_city int(9) DEFAULT NULL COMMENT \'工作地
\',
  born_city int(9) DEFAULT NULL COMMENT \'出生地
\',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin;'; WHILE i< 100
DO IF i< 10 THEN
SET table_name=CONCAT('t_user_0',i); ELSE
SET table_name=CONCAT('t_user_',i); END IF;
SET sql_text=CONCAT('CREATE TABLE ',table_name,table_body);
SELECT sql_text;
SET @sql_text=sql_text; PREPARE stmt FROM @sql_text; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET i=i+1; END WHILE; END $$
DELIMITER ;

--- 用户表结构
CREATE TABLE `t_user`
(
    `user_id`     bigint NOT NULL                                        DEFAULT '-1' COMMENT '用户id',
    `nick_name`   varchar(35) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin  DEFAULT NULL COMMENT '昵称',
    `avatar`      varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像',
    `true_name`   varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin  DEFAULT NULL COMMENT '真实姓名',
    `sex`         tinyint(1) DEFAULT NULL COMMENT '性别 0男，1女',
    `born_date`   datetime                                               DEFAULT NULL COMMENT '出生时间',
    `work_city`   int                                                    DEFAULT NULL COMMENT '工作地',
    `born_city`   int                                                    DEFAULT NULL COMMENT '出生地',
    `create_time` datetime                                               DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime                                               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表';

--- 用户扩展表结构
CREATE TABLE `t_user_phone`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `phone`       varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT '手机号',
    `user_id`     bigint                                                          DEFAULT '-1' COMMENT '用户id',
    `status`      tinyint                                                         DEFAULT '-1' COMMENT '状态(0无效，1有效)',
    `create_time` datetime                                                        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime                                                        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `udx_phone` (`phone`),
    KEY           `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户手机表';
--- 用户扩展表结构
CREATE TABLE `t_user_tag`
(
    `user_id`     bigint NOT NULL DEFAULT '-1' COMMENT '用户id',
    `tag_info_01` bigint NOT NULL DEFAULT '0' COMMENT '标签记录字段',
    `tag_info_02` bigint NOT NULL DEFAULT '0' COMMENT '标签记录字段',
    `tag_info_03` bigint NOT NULL DEFAULT '0' COMMENT '标签记录字段',
    `create_time` datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户标签记录';

CREATE TABLE `t_user_phone` (
                                `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
                                `phone` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '' COMMENT '手机号',
                                `user_id` bigint DEFAULT '-1' COMMENT '用户id',
                                `status` tinyint DEFAULT '-1' COMMENT '状态(0无效，1有效)',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `udx_phone` (`phone`),
                                KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户手机表';


--- 一百张表 手机号
DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `create_t_user_phone_100`()
BEGIN

         DECLARE i INT;
         DECLARE table_name VARCHAR(30);
         DECLARE table_pre VARCHAR(30);
         DECLARE sql_text VARCHAR(3000);
         DECLARE table_body VARCHAR(2000);
         SET i=0;
         SET table_name='';

         SET sql_text='';
         SET table_body = ' (
  id bigint unsigned NOT NULL AUTO_INCREMENT COMMENT \'主键id\',
  phone varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT \'\' COMMENT \'手机号\',
  user_id bigint DEFAULT -1 COMMENT \'用户id\',
  status tinyint DEFAULT -1 COMMENT \'状态(0无效，1有效)\',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT \'创建时间\',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT \'更新时间\',
  PRIMARY KEY (id),
  UNIQUE KEY `udx_phone` (`phone`),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;';

            WHILE i<100 DO
                IF i<10 THEN
                    SET table_name = CONCAT('t_user_phone_0',i);
ELSE
                    SET table_name = CONCAT('t_user_phone_',i);
END IF;

                SET sql_text=CONCAT('CREATE TABLE ',table_name, table_body);
SELECT sql_text;
SET @sql_text=sql_text;
PREPARE stmt FROM @sql_text;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET i=i+1;
END WHILE;


END;;
DELIMITER ;