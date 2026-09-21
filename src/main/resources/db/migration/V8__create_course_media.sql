CREATE TABLE course_media (
                              id               UUID PRIMARY KEY,
                              course_id        UUID         NOT NULL,
                              kind             VARCHAR(20)  NOT NULL,
                              file_name        VARCHAR(255) NOT NULL,
                              content_type     VARCHAR(150) NOT NULL,
                              size_bytes       BIGINT       NOT NULL,
                              duration_seconds INTEGER,
                              sha256           VARCHAR(64),
                              object_key       VARCHAR(512) NOT NULL,
                              upload_id        VARCHAR(512),
                              status           VARCHAR(20)  NOT NULL DEFAULT 'UPLOADING',
                              part_size_bytes  BIGINT       NOT NULL,
                              part_count       INTEGER      NOT NULL,
                              created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                              completed_at     TIMESTAMPTZ,

                              CONSTRAINT fk_course_media_course FOREIGN KEY (course_id)
                                  REFERENCES courses (id) ON DELETE CASCADE,
                              CONSTRAINT chk_course_media_kind   CHECK (kind IN ('VIDEO', 'DOCUMENT')),
                              CONSTRAINT chk_course_media_status CHECK (status IN ('UPLOADING', 'READY', 'FAILED', 'ABORTED'))
);

CREATE INDEX idx_course_media_course ON course_media (course_id);
CREATE INDEX idx_course_media_stale  ON course_media (status, created_at);