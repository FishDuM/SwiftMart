create database if not exists swift_mart;

use swift_mart;

CREATE TABLE `t_user` (
                          `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          `nickname` VARCHAR(32) NOT NULL COMMENT '用户昵称',
                          `password` VARCHAR(72) NOT NULL COMMENT '密码(加密存储)',
                          `mobile` VARCHAR(11) NOT NULL COMMENT '手机号',
                          `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
                          `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
                          `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`id`) USING BTREE,
                          UNIQUE KEY `uk_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `t_goods` (
                           `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                           `goods_name` VARCHAR(200) NOT NULL COMMENT '商品名称(完整，含规格)',
                           `goods_img` VARCHAR(255) NOT NULL COMMENT '商品主图(冗余字段，用于列表展示)',
                           `goods_price` DECIMAL(10,2) NOT NULL COMMENT '商品原价',
                           `status` TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态(0:下架 1:上架)',
                           `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0:未删除 1:已删除)',
                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`) USING BTREE,
                           KEY `idx_status_deleted` (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

CREATE TABLE `t_goods_img` (
                               `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `goods_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
                               `img_url` VARCHAR(255) NOT NULL COMMENT '图片URL',
                               `sort` INT NOT NULL DEFAULT 0 COMMENT '排序(数字越小越靠前)',
                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               PRIMARY KEY (`id`) USING BTREE,
                               KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片表';

CREATE TABLE `t_goods_detail` (
                                  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `goods_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
                                  `detail_content` LONGTEXT DEFAULT NULL COMMENT '商品详情(HTML/富文本)',
                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE KEY `uk_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品详情表';

CREATE TABLE `t_seckill_activity` (
                                      `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '活动ID',
                                      `activity_name` VARCHAR(100) NOT NULL COMMENT '活动名称',
                                      `begin_time` DATETIME NOT NULL COMMENT '活动开始时间',
                                      `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
                                      `status` TINYINT NOT NULL DEFAULT 0 COMMENT '活动状态(0:未开始 1:进行中 2:已结束)',
                                      `description` VARCHAR(500) DEFAULT NULL COMMENT '活动描述',
                                      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      KEY `idx_begin_time` (`begin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';

CREATE TABLE `t_seckill_goods` (
                                   `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `activity_id` BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
                                   `goods_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
                                   `seckill_title` VARCHAR(100) NOT NULL COMMENT '秒杀展示名称(简短，用于列表)',
                                   `seckill_img` VARCHAR(255) NOT NULL COMMENT '秒杀展示图片(用于列表)',
                                   `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
                                   `seckill_total` INT NOT NULL DEFAULT 0 COMMENT '秒杀库存总量(不变)',
                                   `seckill_stock` INT NOT NULL DEFAULT 0 COMMENT '秒杀剩余库存(扣减)',
                                   `seckill_limit` INT NOT NULL DEFAULT 1 COMMENT '秒杀限购数量',
                                   `sort` INT NOT NULL DEFAULT 0 COMMENT '展示排序(数字越小越靠前)',
                                   `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
                                   `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0:未删除 1:已删除)',
                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE KEY `uk_activity_goods` (`activity_id`, `goods_id`),
                                   KEY `idx_activity_sort` (`activity_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀商品关联表';

CREATE TABLE `t_seckill_order` (
                                   `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                   `activity_id` BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
                                   `goods_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
                                   `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
                                   `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价格(冗余字段)',
                                   `goods_name` VARCHAR(100) NOT NULL COMMENT '商品名称(冗余字段)',
                                   `goods_img` VARCHAR(255) NOT NULL COMMENT '商品图片(冗余字段)',
                                   `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态(0:待支付 1:待发货 2:已发货 3:已收货 4:已退款 5:已取消 6:已关闭)',
                                   `expire_time` DATETIME NOT NULL COMMENT '订单过期时间',
                                   `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0:未删除 1:已删除)',
                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE KEY `uk_user_activity_goods` (`user_id`, `activity_id`, `goods_id`),
                                   UNIQUE KEY `uk_order_no` (`order_no`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单表';

-- 商品数据
INSERT INTO t_goods (id, goods_name, goods_img, goods_price, status, is_deleted)
VALUES (1, 'iPhone 15 Pro Max 256GB 深空黑色', 'https://img.quanxiaoha.com/iphone15promax.jpg', 9999.00, 1, 0),
       (2, 'MacBook Pro 14英寸 M3 Pro', 'https://img.quanxiaoha.com/macbookpro14.jpg', 14999.00, 1, 0),
       (3, 'AirPods Pro 2 USB-C', 'https://img.quanxiaoha.com/airpodspro2.jpg', 1899.00, 1, 0);

-- 秒杀活动
INSERT INTO t_seckill_activity (id, activity_name, begin_time, end_time, status, description)
VALUES (1, '618年中大促秒杀专场', '2026-04-01 00:00:00', '2026-12-31 23:59:59', 1, '全场商品低至5折起');

-- 秒杀商品
INSERT INTO t_seckill_goods (id, activity_id, goods_id, seckill_title, seckill_img, seckill_price, seckill_total,
                             seckill_stock, seckill_limit, sort, version, is_deleted)
VALUES (1, 1, 1, '【秒杀】iPhone 15 Pro Max 256GB', 'https://img.quanxiaoha.com/sk-iphone15promax.jpg', 8999.00, 100, 88,
        1, 1, 0, 0),
       (2, 1, 2, '【秒杀】MacBook Pro 14英寸', 'https://img.quanxiaoha.com/sk-macbookpro14.jpg', 12999.00, 50, 42, 1, 2,
        0, 0),
       (3, 1, 3, '【秒杀】AirPods Pro 2', 'https://img.quanxiaoha.com/sk-airpodspro2.jpg', 1499.00, 200, 166, 1, 3, 0, 0);

-- 为商品 ID=1 (iPhone 15 Pro Max 256GB) 插入轮播图
INSERT INTO t_goods_img (goods_id, img_url, sort)
VALUES (1, 'https://img.quanxiaoha.com/goods/iphone15promax-1.jpg', 1),
       (1, 'https://img.quanxiaoha.com/goods/iphone15promax-2.jpg', 2),
       (1, 'https://img.quanxiaoha.com/goods/iphone15promax-3.jpg', 3),
       (1, 'https://img.quanxiaoha.com/goods/iphone15promax-4.jpg', 4);

-- 为商品 ID=2 (MacBook Pro 14英寸 M3 Pro) 插入轮播图
INSERT INTO t_goods_img (goods_id, img_url, sort)
VALUES (2, 'https://img.quanxiaoha.com/goods/macbookpro14-1.jpg', 1),
       (2, 'https://img.quanxiaoha.com/goods/macbookpro14-2.jpg', 2),
       (2, 'https://img.quanxiaoha.com/goods/macbookpro14-3.jpg', 3);

-- 为商品 ID=1 (iPhone 15 Pro Max 256GB) 插入详情
INSERT INTO t_goods_detail (goods_id, detail_content)
VALUES (1,
        '<h2>iPhone 15 Pro Max 256GB</h2><p>A17 Pro 芯片，钛金属设计，4800 万像素主摄。</p><ul><li>6.7 英寸超视网膜 XDR 显示屏</li><li>最长 29 小时视频播放</li><li>支持 USB 3，传输速度最高可达 10Gb/s</li></ul>');

-- 为商品 ID=2 (MacBook Pro 14英寸 M3 Pro) 插入详情
INSERT INTO t_goods_detail (goods_id, detail_content)
VALUES (2,
        '<h2>MacBook Pro 14英寸 M3 Pro</h2><p>M3 Pro 芯片，14 英寸 Liquid Retina XDR 显示屏。</p><ul><li>最高 18 小时电池续航</li><li>最高 36GB 统一内存</li><li>三个 Thunderbolt 4 端口</li></ul>');
