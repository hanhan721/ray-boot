-- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    role_id         BIGINT          NOT NULL,                               -- 角色ID
    permission_id   BIGINT          NOT NULL,                               -- 权限ID
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_sys_role_permission_permission_id ON sys_role_permission (permission_id);
