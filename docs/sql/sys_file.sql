-- 未上线项目的初始化定义；已有开发库须显式重建，应用不会自动清空数据或存储。
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS sys_file_object;
DROP TYPE IF EXISTS storage_type;
CREATE TYPE storage_type AS ENUM ('LOCAL', 'RUST_FS');

CREATE TABLE sys_file_object (
    id BIGINT PRIMARY KEY,
    storage_id VARCHAR(64) NOT NULL,
    storage_type storage_type NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL CHECK (file_hash ~ '^[0-9a-f]{64}$'),
    object_key VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    content_type VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','DELETING','PURGED')),
    unreferenced_since TIMESTAMP,
    delete_started_at TIMESTAMP,
    purged_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (storage_id, object_key)
);
CREATE UNIQUE INDEX uk_sys_file_object_available ON sys_file_object(storage_id, file_hash) WHERE status = 'AVAILABLE';
CREATE INDEX idx_sys_file_object_gc ON sys_file_object(status, unreferenced_since, id);

CREATE OR REPLACE FUNCTION guard_file_object() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'file object history must be retained';
    END IF;
    IF ROW(NEW.storage_id, NEW.storage_type, NEW.bucket, NEW.file_hash, NEW.object_key,
           NEW.file_size, NEW.content_type, NEW.create_time)
       IS DISTINCT FROM ROW(OLD.storage_id, OLD.storage_type, OLD.bucket, OLD.file_hash, OLD.object_key,
           OLD.file_size, OLD.content_type, OLD.create_time) THEN
        RAISE EXCEPTION 'file object identity is immutable';
    END IF;
    IF NEW.status <> OLD.status AND NOT
       ((OLD.status = 'AVAILABLE' AND NEW.status = 'DELETING') OR
        (OLD.status = 'DELETING' AND NEW.status = 'PURGED')) THEN
        RAISE EXCEPTION 'invalid file object state transition';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_file_object BEFORE UPDATE OR DELETE ON sys_file_object
    FOR EACH ROW EXECUTE FUNCTION guard_file_object();

CREATE TABLE sys_file (
    id BIGINT PRIMARY KEY,
    object_id BIGINT NOT NULL REFERENCES sys_file_object(id) ON DELETE RESTRICT,
    original_name VARCHAR(255) NOT NULL,
    file_ext VARCHAR(20) NOT NULL,
    biz_type VARCHAR(50),
    biz_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT NOT NULL,
    update_by BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    CHECK ((biz_type IS NULL) = (biz_id IS NULL)),
    CHECK (biz_type IS NULL OR length(trim(biz_type)) > 0),
    CHECK (deleted = (deleted_at IS NOT NULL))
);
CREATE INDEX idx_sys_file_biz ON sys_file(biz_type, biz_id);
CREATE INDEX idx_sys_file_active_object ON sys_file(object_id) WHERE NOT deleted;
CREATE INDEX idx_sys_file_unbound ON sys_file(create_time, id) WHERE NOT deleted AND biz_type IS NULL;

CREATE OR REPLACE FUNCTION guard_logical_file() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.deleted AND NOT NEW.deleted THEN RAISE EXCEPTION 'deleted files cannot be restored'; END IF;
    IF NEW.object_id <> OLD.object_id OR NEW.create_by <> OLD.create_by THEN
        RAISE EXCEPTION 'logical file identity is immutable';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_logical_file BEFORE UPDATE ON sys_file
    FOR EACH ROW EXECUTE FUNCTION guard_logical_file();
