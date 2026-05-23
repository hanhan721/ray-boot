-- 登录日志表
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id              BIGINT          NOT NULL,                               -- 日志ID
    username        VARCHAR(50),                                            -- 用户名
    login_ip        VARCHAR(45),                                            -- 登录IP
    login_location  VARCHAR(255),                                           -- 登录地点
    browser         VARCHAR(100),                                           -- 浏览器
    os              VARCHAR(100),                                           -- 操作系统
    success         BOOLEAN         DEFAULT TRUE,                           -- 是否成功
    msg             VARCHAR(255),                                           -- 提示消息
    login_time      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 登录时间
    PRIMARY KEY (id)
);

CREATE INDEX idx_sys_login_log_username ON sys_login_log (username);
CREATE INDEX idx_sys_login_log_login_time ON sys_login_log (login_time);
