-- 创建数据库
CREATE DATABASE IF NOT EXISTS route_planner DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE route_planner;

-- POI 表（兴趣点）
DROP TABLE IF EXISTS `poi`;
CREATE TABLE `poi` (
    `id` VARCHAR(64) PRIMARY KEY COMMENT 'POI ID (UUID)',
    `name` VARCHAR(128) NOT NULL COMMENT '名称',
    `address` VARCHAR(256) COMMENT '地址',
    `category` VARCHAR(32) NOT NULL COMMENT '类别: RESTAURANT/ATTRACTION/PARK/MUSEUM/SHOPPING/CAFE',
    `sub_category` VARCHAR(64) COMMENT '子类别',
    `city` VARCHAR(64) DEFAULT '杭州' COMMENT '城市',
    `district` VARCHAR(64) COMMENT '区县',
    
    -- 地理信息
    `lat` DECIMAL(10, 6) COMMENT '纬度',
    `lng` DECIMAL(10, 6) COMMENT '经度',
    
    -- 时间信息
    `recommended_duration` INT DEFAULT 90 COMMENT '建议游玩时长(分钟)',
    `opening_hours` VARCHAR(64) DEFAULT '09:00-17:00' COMMENT '营业时间',
    `peak_hours` VARCHAR(128) DEFAULT NULL COMMENT '高峰时段(JSON)',
    
    -- 评分与价格
    `rating` DECIMAL(2, 1) DEFAULT 4.0 COMMENT '综合评分(1-5)',
    `review_count` INT DEFAULT 0 COMMENT '评价数量',
    `price_level` TINYINT DEFAULT 2 COMMENT '价格等级(1-5)',
    `avg_cost` DECIMAL(8, 2) DEFAULT 0.00 COMMENT '人均消费(元)',
    
    -- 动态信息
    `avg_wait_time` INT DEFAULT 0 COMMENT '当前排队时间(分钟)',
    `current_crowd_level` TINYINT DEFAULT 50 COMMENT '当前拥挤度(0-100)',
    `crowd_updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '拥挤度更新时间',
    
    -- 标签与特征
    `tags` TEXT COMMENT '标签列表(JSON数组)',
    `facilities` TEXT COMMENT '设施列表(JSON数组)',
    
    -- 来源与时间戳
    `data_source` VARCHAR(32) DEFAULT 'manual' COMMENT '数据来源',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1=正常, 0=停用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX `idx_category` (`category`),
    INDEX `idx_rating` (`rating`),
    INDEX `idx_location` (`lat`, `lng`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兴趣点表';

-- 插入示例数据（杭州市热门景点）
INSERT INTO `poi` (`id`, `name`, `address`, `category`, `city`, `district`, `lat`, `lng`, 
                   `recommended_duration`, `opening_hours`, `rating`, `review_count`, 
                   `price_level`, `avg_cost`, `current_crowd_level`, `tags`, `facilities`) VALUES

-- 公园类
('poi_001', '西湖风景区', '浙江省杭州市西湖区龙井路1号', 'PARK', '杭州', '西湖区', 30.2545, 120.1489, 180, '全天开放', 4.8, 12580, 1, 0.00, 45, '["自然风光", "免费", "网红打卡", "适合拍照", "亲子"]', '["停车场", "游船", "餐厅", "洗手间"]'),
('poi_002', '太子湾公园', '浙江省杭州市西湖区南山路', 'PARK', '杭州', '西湖区', 30.2436, 120.1445, 60, '08:00-17:30', 4.6, 5620, 1, 0.00, 35, '["樱花", "郁金香", "免费", "春季限定", "情侣约会"]', '["停车场", "自动售货机"]'),
('poi_003', '曲院风荷', '浙江省杭州市西湖区灵隐路', 'PARK', '杭州', '西湖区', 30.2501, 120.1378, 90, '全天开放', 4.7, 3890, 1, 0.00, 40, '["荷花", "免费", "夏季限定", "摄影"]', '["停车场", "茶室"]'),

-- 博物馆类
('poi_004', '浙江省博物馆', '浙江省杭州市西湖区孤山路25号', 'MUSEUM', '杭州', '西湖区', 30.2504, 120.1421, 120, '09:00-17:00(周一闭馆)', 4.6, 7820, 1, 0.00, 55, '["历史文化", "免费", "教育", "室内活动", "雨天备选"]', '["无障碍通道", "语音导览", "咖啡厅", "纪念品商店"]'),
('poi_005', '中国丝绸博物馆', '浙江省杭州市西湖区玉皇山路73-1号', 'MUSEUM', '杭州', '西湖区', 30.2285, 120.1482, 90, '09:00-17:00(周一闭馆)', 4.5, 3420, 1, 0.00, 30, '["丝绸文化", "免费", "艺术", "小众"]', '["无障碍通道", "停车场"]'),

-- 餐厅类
('poi_006', '楼外楼', '浙江省杭州市西湖区孤山路30号', 'RESTAURANT', '杭州', '西湖区', 30.2508, 120.1428, 90, '11:00-21:00', 4.5, 8920, 4, 158.00, 65, '["杭帮菜", "老字号", "西湖醋鱼", "名店", "游客必去"]', '["包厢", "停车位", "无障碍通道"]'),
('poi_007', '外婆家', '浙江省杭州市西湖区湖滨银泰in77', 'RESTAURANT', '杭州', '上城区', 30.2548, 120.1656, 75, '11:00-21:30', 4.3, 12540, 3, 68.00, 70, '["杭帮菜", "性价比高", "连锁", "排队热门"]', '["外卖", "包厢"]'),
('poi_008', '新白鹿', '浙江省杭州市上城区延安路98号', 'RESTAURANT', '杭州', '上城区', 30.2562, 120.1689, 70, '11:00-22:00', 4.2, 9870, 2, 55.00, 60, '["杭帮菜", "性价比超高", "年轻人喜爱"]', '["外卖", "包厢"]'),

-- 景点类
('poi_009', '灵隐寺', '浙江省杭州市西湖区法云弄1号', 'ATTRACTION', '杭州', '西湖区', 30.2436, 120.1016, 150, '07:00-18:15', 4.7, 15680, 3, 75.00, 75, '["佛教文化", "古刹", "祈福", "历史悠久的", "必去景点"]', '["停车场", "素斋餐厅", "香火处", "导游服务"]'),
('poi_010', '雷峰塔', '浙江省杭州市西湖区南山路15号', 'ATTRACTION', '杭州', '西湖区', 30.2345, 120.1498, 90, '08:00-20:30', 4.4, 11230, 3, 40.00, 68, '["古迹", "登高望远", "白娘子传说", "日落观赏"]', '["电梯", "停车场", "纪念品商店"]'),
('poi_011', '三潭印月', '浙江省杭州市西湖区西湖水域中心', 'ATTRACTION', '杭州', '西湖区', 30.2478, 120.1456, 60, '08:00-17:00', 4.6, 8950, 2, 55.00, 50, '["西湖十景之一", "游船", "人民币背景图案", "经典打卡"]', '["游船码头", "洗手间"]'),

-- 咖啡类
('poi_012', '星巴克(湖滨店)', '浙江省杭州市上城区湖滨路28号', 'CAFE', '杭州', '上城区', 30.2556, 120.1645, 45, '07:00-23:00', 4.2, 4560, 3, 42.00, 55, '["咖啡连锁", "办公", "会议", "环境好"]', '["WiFi", "电源插座", "室外座位"]'),
('poi_013', '漫咖啡(西湖店)', '浙江省杭州市西湖区北山街', 'CAFE', '杭州', '西湖区', 30.2578, 120.1467, 60, '09:00-23:00', 4.4, 2340, 3, 58.00, 35, '["韩式咖啡", "环境优雅", "适合聊天", "网红"]', '["WiFi", "停车场", "书架"]'),
('poi_014', '瑞幸咖啡(龙翔桥店)', '浙江省杭州市上城区平海路', 'CAFE', '杭州', '上城区', 30.2562, 120.1634, 20, '07:00-22:00', 4.0, 1890, 2, 18.00, 45, '["性价比高", "快速", "外带", "上班族"]', '["自提柜"]'),

-- 购物类
('poi_015', '湖滨银泰in77', '浙江省杭州市上城区延安路258号', 'SHOPPING', '杭州', '上城区', 30.2554, 120.1662, 180, '10:00-22:00', 4.5, 9870, 4, 200.00, 72, '["大型商场", "奢侈品", "餐饮", "娱乐", "一站式购物"]', '["停车场", "母婴室", "休息区", "WiFi"]'),
('poi_016', '河坊街', '浙江省杭州市上城区中山南路', 'SHOPPING', '杭州', '上城区', 30.2445, 120.1698, 120, '09:00-21:00', 4.3, 7650, 2, 100.00, 68, '["特色商业街", "小吃", "工艺品", "传统建筑", "游客聚集"]', '["停车场", "洗手间", "特产商店"]');

SELECT CONCAT('✅ 数据库初始化完成！已插入 ', COUNT(*), ' 条POI数据') AS message FROM poi;
