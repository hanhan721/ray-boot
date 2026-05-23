-- 定时任务表
DROP TABLE IF EXISTS sys_job;
CREATE TABLE sys_job (
    id              BIGINT          NOT NULL,                               -- 主键
    job_name        VARCHAR(64)     NOT NULL,                               -- 任务名称
    job_group       VARCHAR(64)     NOT NULL DEFAULT 'DEFAULT',             -- 任务分组
    bean_name       VARCHAR(200)    NOT NULL,                               -- Spring Bean名称
    method_name     VARCHAR(200)    NOT NULL,                               -- 方法名称
    method_params   VARCHAR(500),                                           -- 方法参数
    cron_expression VARCHAR(255)    NOT NULL,                               -- Cron表达式
    concurrent      BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否允许并发
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,                  -- 是否启用
    remark          VARCHAR(500),                                           -- 备注
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_job_name_group ON sys_job (job_name, job_group) WHERE NOT deleted;
