-- 文件管理表
DROP TYPE IF EXISTS storage_type;
CREATE TYPE storage_type AS ENUM ('LOCAL', 'RUST_FS');

DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
    id              BIGINT          NOT NULL,                               -- 主键
    original_name   VARCHAR(255)    NOT NULL,                               -- 原始文件名
    file_name       VARCHAR(255)    NOT NULL,                               -- 存储文件名（UUID.ext）
    file_path       VARCHAR(500)    NOT NULL,                               -- 存储路径（相对路径，如 2026/02/12/uuid.jpg）
    file_ext        VARCHAR(20),                                            -- 文件扩展名（不含点号）
    file_size       BIGINT          NOT NULL,                               -- 文件大小（字节）
    content_type    VARCHAR(100),                                           -- MIME 类型
    storage_type    storage_type    NOT NULL DEFAULT 'LOCAL',               -- 存储类型
    bucket          VARCHAR(100),                                           -- 存储桶（MinIO/RustFS 使用，本地存储为空）
    biz_type        VARCHAR(50),                                            -- 业务类型（如 USER_AVATAR, CONTRACT）
    biz_id          BIGINT,                                                 -- 业务关联 ID
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    create_by       BIGINT,                                                 -- 创建人 ID
    update_by       BIGINT,                                                 -- 更新人 ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 是否删除
    PRIMARY KEY (id)
);

CREATE INDEX idx_sys_file_biz ON sys_file (biz_type, biz_id);
CREATE INDEX idx_sys_file_storage_type ON sys_file (storage_type);
CREATE INDEX idx_sys_file_create_time ON sys_file (create_time);
