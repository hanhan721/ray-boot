-- 字典数据表
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
    id              BIGINT          NOT NULL,                               -- 字典数据ID
    dict_type       VARCHAR(100)    NOT NULL,                               -- 字典类型
    dict_label      VARCHAR(100)    NOT NULL,                               -- 字典标签
    dict_value      VARCHAR(100)    NOT NULL,                               -- 字典值
    sort            INT             NOT NULL DEFAULT 0,                     -- 排序
    css_class       VARCHAR(100),                                           -- CSS类名
    list_class      VARCHAR(100),                                           -- 列表类名
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否默认
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,                  -- 是否启用
    remark          VARCHAR(500),                                           -- 备注
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE INDEX idx_sys_dict_data_dict_type ON sys_dict_data (dict_type);
