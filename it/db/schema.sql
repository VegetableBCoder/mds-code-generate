-- ============================================================================
-- codegen 链路验证库（MariaDB / MySQL 8 兼容）
--
-- 刻意覆盖这些形态：
--   * BIGINT 主键（字段名 id）      → 插件会注入 idGenerator.nextId()
--   * VARCHAR 业务主键（order_no）  → 主键由调用方给，不注入生成器
--   * CHAR(36) 主键（uuid）         → 同上，且类型不是 Long
--   * SMALLINT 枚举列               → DTO 里换成枚举类型，converter 生成双向转换
--   * TINYINT / DECIMAL / TEXT / TIMESTAMP / DATETIME(3) → 常见类型都要能过
-- ============================================================================

CREATE DATABASE IF NOT EXISTS codegen_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE codegen_test;

DROP TABLE IF EXISTS app_user;
CREATE TABLE app_user (
  id            BIGINT       NOT NULL                COMMENT '主键（应用侧雪花 ID，非自增）',
  username      VARCHAR(64)  NOT NULL                COMMENT '登录名',
  display_name  VARCHAR(128)     NULL                COMMENT '展示名',
  password_hash VARCHAR(255)     NULL                COMMENT '凭据列（DTO 不裁剪，由业务层决定是否外露）',
  enabled       TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用',
  status        SMALLINT     NOT NULL DEFAULT 1      COMMENT '账号状态：1 正常 / 2 停用 / 3 锁定',
  balance       DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '余额',
  profile       TEXT             NULL                COMMENT '扩展信息（JSON 文本）',
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号';

DROP TABLE IF EXISTS order_record;
CREATE TABLE order_record (
  order_no   VARCHAR(40)   NOT NULL COMMENT '业务单号（主键，由调用方给，不由插件生成）',
  user_id    BIGINT        NOT NULL COMMENT '下单账号',
  amount     DECIMAL(18,4) NOT NULL COMMENT '金额',
  state      SMALLINT      NOT NULL COMMENT '订单状态：1 新建 / 2 已支付 / 3 已取消',
  channel    VARCHAR(32)       NULL COMMENT '渠道',
  payload    TEXT              NULL COMMENT '下单快照（JSON 文本）',
  created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (order_no),
  KEY idx_order_record_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

DROP TABLE IF EXISTS device;
CREATE TABLE device (
  uuid      CHAR(36)   NOT NULL COMMENT '设备唯一标识（主键）',
  name      VARCHAR(100) NOT NULL COMMENT '设备名',
  kind      SMALLINT   NOT NULL COMMENT '设备类型：1 真机 / 2 模拟器 / 3 云机',
  online    TINYINT    NOT NULL DEFAULT 0 COMMENT '是否在线',
  last_seen DATETIME       NULL COMMENT '最后心跳',
  PRIMARY KEY (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备';
