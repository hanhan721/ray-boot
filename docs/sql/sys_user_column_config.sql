-- 用户列配置表
DROP TABLE IF EXISTS sys_user_column_config;
CREATE TABLE sys_user_column_config (
    id          BIGINT        NOT NULL,                               -- 主键
    user_id     BIGINT        NOT NULL,                               -- 用户ID
    page_key    VARCHAR(100)  NOT NULL,                               -- 页面标识，如 sys:user, order:list
    columns     JSONB         NOT NULL,                               -- 列配置JSON数组
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_user_page ON sys_user_column_config (user_id, page_key);
