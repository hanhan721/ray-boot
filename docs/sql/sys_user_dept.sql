-- 用户部门关联表
DROP TABLE IF EXISTS sys_user_dept;
CREATE TABLE sys_user_dept (
    user_id         BIGINT          NOT NULL,                               -- 用户ID
    dept_id         BIGINT          NOT NULL,                               -- 部门ID
    PRIMARY KEY (user_id, dept_id)
);

CREATE INDEX idx_sys_user_dept_dept_id ON sys_user_dept (dept_id);
