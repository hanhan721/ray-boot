-- 字典类型表
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
    id              BIGINT          NOT NULL,                               -- 字典类型ID
    dict_name       VARCHAR(100)    NOT NULL,                               -- 字典名称
    dict_type       VARCHAR(100)    NOT NULL,                               -- 字典类型
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,                  -- 是否启用
    remark          VARCHAR(500),                                           -- 备注
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_dict_type_dict_type ON sys_dict_type (dict_type) WHERE NOT deleted;
