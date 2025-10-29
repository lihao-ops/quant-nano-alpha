-- 每日稳定股票精选表
CREATE TABLE IF NOT EXISTS `strategy_daily_picks` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
  `trade_date` DATE NOT NULL COMMENT '交易日期',
  `strategy_id` VARCHAR(64) NOT NULL COMMENT '策略ID',
  `stock_code` VARCHAR(20) NOT NULL COMMENT '股票代码',
  `stock_name` VARCHAR(100) COMMENT '股票名称',
  `industry` VARCHAR(50) COMMENT '所属行业',
  `score` DECIMAL(10,4) COMMENT '策略评分',
  `ranking` INT COMMENT '当日排名',
  `market_cap` DECIMAL(20,2) COMMENT '市值(亿)',
  `pe_ratio` DECIMAL(10,2) COMMENT '市盈率',
  `extra_data` JSON COMMENT '扩展字段(指标详情)',
  `version` INT DEFAULT 1 COMMENT '数据版本号(乐观锁)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_strategy_stock` (`trade_date`, `strategy_id`, `stock_code`),
  KEY `idx_trade_date` (`trade_date`),
  KEY `idx_strategy_date` (`strategy_id`, `trade_date`),
  KEY `idx_industry` (`industry`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日稳定策略精选股票表';

-- 交易日历表
CREATE TABLE IF NOT EXISTS `trade_calendar` (
  `id` INT UNSIGNED AUTO_INCREMENT,
  `trade_date` DATE NOT NULL COMMENT '交易日',
  `is_open` TINYINT(1) DEFAULT 1 COMMENT '是否开市',
  `market` VARCHAR(10) DEFAULT 'CN' COMMENT '市场(CN/US/HK)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_market` (`trade_date`, `market`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易日历';

-- 策略元数据表
CREATE TABLE IF NOT EXISTS `strategy_meta` (
  `strategy_id` VARCHAR(64) PRIMARY KEY,
  `strategy_name` VARCHAR(200),
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态: 1-启用 0-禁用',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略元信息';

-- 缓存刷新日志表
CREATE TABLE IF NOT EXISTS `cache_refresh_log` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT,
  `cache_key` VARCHAR(200) NOT NULL COMMENT '缓存Key',
  `operation` VARCHAR(20) COMMENT 'EVICT/REFRESH/WARMUP',
  `trigger_source` VARCHAR(50) COMMENT '触发源: KAFKA/SCHEDULE/MANUAL',
  `status` VARCHAR(20) COMMENT 'SUCCESS/FAILED',
  `error_msg` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cache_key` (`cache_key`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缓存刷新日志';
