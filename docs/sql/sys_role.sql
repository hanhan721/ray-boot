-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id              BIGINT          NOT NULL,                               -- 主键
    role_name       VARCHAR(50)     NOT NULL,                               -- 角色名称
    role_key        VARCHAR(100)    NOT NULL,                               -- 角色标识
    sort            INT             NOT NULL DEFAULT 0,                     -- 显示顺序
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,                  -- 是否启用
    remark          VARCHAR(500),                                           -- 备注
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_role_role_key ON sys_role (role_key) WHERE NOT deleted;
