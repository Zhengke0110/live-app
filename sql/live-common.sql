CREATE TABLE `t_id_generate_config`
(
    `id`             INT NOT NULL AUTO_INCREMENT COMMENT '主键 id',
    `remark`         VARCHAR(255) CHARACTER
                         SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
    `next_threshold` BIGINT                                     DEFAULT NULL COMMENT '当前 id 所在阶段的阈
	值',
    `init_num`       BIGINT                                     DEFAULT NULL COMMENT '初始化值',
    `current_start`  BIGINT                                     DEFAULT NULL COMMENT '当前 id 所在阶段的开始
	值',
    `step`           INT                                        DEFAULT NULL COMMENT 'id 递增区间',
    `is_seq`         TINYINT                                    DEFAULT NULL COMMENT '是否有序（0 无序，1 有序）',
    `id_prefix`      VARCHAR(60) CHARACTER
                         SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务前缀码，如果没有则返回
	时不携带',
    `version`        INT NOT NULL                               DEFAULT '0' COMMENT '乐观锁版本号',
    `create_time`    DATETIME                                   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时
	间',
    `update_time`    DATETIME                                   DEFAULT CURRENT_TIMESTAMP ON
UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;