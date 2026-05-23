-- 系统参数表
DROP TYPE IF EXISTS param_type;
CREATE TYPE param_type AS ENUM ('STRING', 'INTEGER', 'BOOLEAN', 'JSON', 'STRING_LIST');

DROP TABLE IF EXISTS sys_param;
CREATE TABLE sys_param (
    id              BIGINT          NOT NULL,                               -- 主键
    param_key       VARCHAR(64)     NOT NULL,                               -- 参数键（唯一标识）
    param_value     TEXT            NOT NULL,                               -- 参数值（数据库统一存字符串）
    param_type      param_type      NOT NULL DEFAULT 'STRING',              -- 参数类型
    biz_type        INT             NOT NULL DEFAULT 0,                     -- 业务分类：0=系统，1=用户，2=订单，3=支付，4=通知，99=其他
    encrypted       BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否加密
    description     VARCHAR(256),                                           -- 参数描述
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_param_key ON sys_param (param_key) WHERE NOT deleted;
CREATE INDEX idx_sys_param_biz_type ON sys_param (biz_type);
CREATE INDEX idx_sys_param_create_time ON sys_param (create_time);
