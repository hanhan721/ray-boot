-- 操作日志表
CREATE TYPE business_type AS ENUM ('OTHER', 'INSERT', 'UPDATE', 'DELETE', 'GRANT', 'EXPORT', 'IMPORT');
CREATE TYPE operator_type AS ENUM ('OTHER', 'BACKEND', 'MOBILE');

DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    id              BIGINT              NOT NULL,                               -- 日志ID
    title           VARCHAR(100),                                               -- 模块标题
    biz_type        business_type       NOT NULL DEFAULT 'OTHER',               -- 业务类型
    method          VARCHAR(200),                                               -- 方法名称
    request_method  VARCHAR(10),                                                -- 请求方式
    oper_type       operator_type       NOT NULL DEFAULT 'OTHER',               -- 操作者类型
    oper_user_id    BIGINT,                                                     -- 操作用户ID
    oper_user_name  VARCHAR(50),                                                -- 操作用户名
    dept_name       VARCHAR(50),                                                -- 部门名称
    oper_url        VARCHAR(500),                                               -- 请求URL
    oper_ip         VARCHAR(45),                                                -- 操作IP
    oper_location   VARCHAR(255),                                               -- 操作地点
    oper_param      TEXT,                                                       -- 请求参数
    json_result     TEXT,                                                       -- 返回结果
    success         BOOLEAN             DEFAULT TRUE,                           -- 是否成功
    error_msg       TEXT,                                                       -- 错误消息
    oper_time       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 操作时间
    cost_time       INT                 DEFAULT 0,                              -- 消耗时间（毫秒）
    PRIMARY KEY (id)
);

CREATE INDEX idx_sys_oper_log_biz_type ON sys_oper_log (biz_type);
CREATE INDEX idx_sys_oper_log_oper_time ON sys_oper_log (oper_time);
CREATE INDEX idx_sys_oper_log_oper_user_id ON sys_oper_log (oper_user_id);
