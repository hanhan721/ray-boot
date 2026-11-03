-- 在部署新应用之前执行。历史记录保留到原过期时间，仅供登出定位。
BEGIN;
ALTER TABLE sys_refresh_token ADD COLUMN IF NOT EXISTS login_session_id VARCHAR(64);
ALTER TABLE sys_refresh_token ADD COLUMN IF NOT EXISTS rotated BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE sys_refresh_token SET login_session_id = session_id WHERE login_session_id IS NULL;
ALTER TABLE sys_refresh_token ALTER COLUMN login_session_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_current_login ON sys_refresh_token (login_session_id) WHERE rotated = FALSE;
CREATE INDEX IF NOT EXISTS idx_refresh_login_session ON sys_refresh_token (login_session_id);
COMMIT;
