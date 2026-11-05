-- 用户表
CREATE TYPE gender AS ENUM ('SECRET', 'MALE', 'FEMALE');

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL,                               -- 主键
    username        VARCHAR(50)     NOT NULL,                               -- 用户名
    password        VARCHAR(100)    NOT NULL,                               -- 密码
    nickname        VARCHAR(50),                                            -- 昵称
    email           VARCHAR(100),                                           -- 邮箱
    phone           VARCHAR(20),                                            -- 手机号
    avatar_file_id  BIGINT REFERENCES sys_file(id) ON DELETE RESTRICT,       -- 头像逻辑文件
    gender          gender          NOT NULL DEFAULT 'SECRET',              -- 性别
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,                  -- 是否启用
    remark          VARCHAR(500),                                           -- 备注
    last_login_time TIMESTAMP,                                              -- 最后登录时间
    last_login_ip   VARCHAR(45),                                            -- 最后登录IP
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username) WHERE NOT deleted;
CREATE INDEX idx_sys_user_create_time ON sys_user (create_time);
