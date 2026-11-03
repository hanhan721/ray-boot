-- Refresh Token 表
DROP TABLE IF EXISTS sys_refresh_token;
CREATE TABLE sys_refresh_token (
    id              BIGINT          NOT NULL,                               -- 主键（雪花ID，应用层生成）
    token_hash      VARCHAR(64)     NOT NULL,                               -- Token的SHA-256哈希值
    user_id         BIGINT          NOT NULL,                               -- 用户ID
    session_id      VARCHAR(64)     NOT NULL,                               -- 关联的会话摘要
    expire_time     TIMESTAMP       NOT NULL,                               -- 过期时间
    user_agent      VARCHAR(500),                                           -- 设备信息
    ip              VARCHAR(50),                                            -- 登录IP
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_token_hash ON sys_refresh_token (token_hash);
CREATE UNIQUE INDEX uk_session_id ON sys_refresh_token (session_id);
CREATE INDEX idx_user_id ON sys_refresh_token (user_id);
CREATE INDEX idx_expire_time ON sys_refresh_token (expire_time);
