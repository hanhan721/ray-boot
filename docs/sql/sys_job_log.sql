-- 任务执行日志表
DROP TABLE IF EXISTS sys_job_log;
CREATE TABLE sys_job_log (
    id              BIGINT          NOT NULL,                               -- 日志ID（雪花ID，应用层生成）
    job_id          BIGINT          NOT NULL,                               -- 任务ID
    job_name        VARCHAR(64),                                            -- 任务名称
    job_group       VARCHAR(64),                                            -- 任务分组
    bean_name       VARCHAR(200),                                           -- Spring Bean名称
    method_name     VARCHAR(200),                                           -- 方法名称
    method_params   VARCHAR(500),                                           -- 方法参数
    success         BOOLEAN         DEFAULT TRUE,                           -- 是否成功
    result          TEXT,                                                   -- 执行结果
    error_msg       TEXT,                                                   -- 错误消息
    cost_time       INT             DEFAULT 0,                              -- 消耗时间（毫秒）
    start_time      TIMESTAMP       NOT NULL,                               -- 开始时间
    end_time        TIMESTAMP,                                              -- 结束时间
    PRIMARY KEY (id)
);

CREATE INDEX idx_sys_job_log_job_id ON sys_job_log (job_id);
CREATE INDEX idx_sys_job_log_start_time ON sys_job_log (start_time);
