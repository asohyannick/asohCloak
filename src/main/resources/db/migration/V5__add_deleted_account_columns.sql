ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_users_deleted_email ON users (deleted_email);