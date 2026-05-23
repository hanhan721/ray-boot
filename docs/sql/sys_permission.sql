-- 权限表（树形结构，支持动态路由）
CREATE TYPE permission_type AS ENUM ('DIRECTORY', 'MENU', 'BUTTON');

DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id              BIGINT              NOT NULL,                               -- 权限ID
    parent_id       BIGINT              DEFAULT 0,                              -- 父级ID，根节点为0
    name            VARCHAR(100)        NOT NULL,                               -- 权限名称/菜单标题
    type            permission_type     NOT NULL,                               -- 权限类型
    url             VARCHAR(255),                                               -- 路由路径
    component       VARCHAR(255),                                               -- 组件路径，目录可为空或Layout
    redirect        VARCHAR(255),                                               -- 重定向地址，目录使用
    code            VARCHAR(100),                                               -- 权限标识，如 user:add，按钮必填
    icon            VARCHAR(100),                                               -- 菜单图标
    sort            INT                 DEFAULT 0,                              -- 排序
    hidden          BOOLEAN             DEFAULT FALSE,                          -- 是否隐藏路由
    keep_alive      BOOLEAN             DEFAULT FALSE,                          -- 是否缓存页面
    enabled         BOOLEAN             NOT NULL DEFAULT TRUE,                  -- 是否启用
    route_name      VARCHAR(64),                                                -- 前端路由名称（vue-router name）
    description     VARCHAR(500),                                               -- 描述
    create_time     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                     -- 创建人ID
    update_by       BIGINT,                                                     -- 更新人ID
    deleted         BOOLEAN             NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

-- code 可为 NULL（目录/菜单不需要），仅对非 NULL 的 code 保证唯一性
CREATE UNIQUE INDEX uk_permission_code ON sys_permission (code) WHERE NOT deleted AND code IS NOT NULL;
CREATE INDEX idx_sys_permission_parent_id ON sys_permission (parent_id);
CREATE INDEX idx_sys_permission_type ON sys_permission (type);
