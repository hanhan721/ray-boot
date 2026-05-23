-- 部门表（树形结构）
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id              BIGINT          NOT NULL,                               -- 部门ID
    dept_name       VARCHAR(50)     NOT NULL,                               -- 部门名称
    parent_id       BIGINT          DEFAULT 0,                              -- 父部门ID，根节点为0
    sort            INT             NOT NULL DEFAULT 0,                     -- 排序
    leader          VARCHAR(50),                                            -- 负责人
    phone           VARCHAR(20),                                            -- 联系电话
    email           VARCHAR(100),                                           -- 邮箱
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,                  -- 是否启用
    remark          VARCHAR(500),                                           -- 备注
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE INDEX idx_sys_dept_parent_id ON sys_dept (parent_id);
