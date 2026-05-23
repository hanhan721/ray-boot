-- 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id         BIGINT          NOT NULL,                               -- 用户ID
    role_id         BIGINT          NOT NULL,                               -- 角色ID
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_sys_user_role_role_id ON sys_user_role (role_id);
