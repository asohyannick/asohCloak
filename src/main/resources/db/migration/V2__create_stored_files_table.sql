CREATE TABLE IF NOT EXISTS stored_files (
    id             UUID PRIMARY KEY,
    object_key     VARCHAR(500)  NOT NULL,
    bucket_name    VARCHAR(100)  NOT NULL,
    content_type   VARCHAR(100),
    presigned_url  TEXT          NOT NULL,
    expires_at     TIMESTAMPTZ   NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_stored_files_object_key ON stored_files (object_key);
CREATE INDEX IF NOT EXISTS idx_stored_files_expires_at ON stored_files (expires_at);