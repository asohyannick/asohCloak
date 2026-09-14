CREATE TABLE courses (
                         id                  UUID PRIMARY KEY,
                         name                VARCHAR(200)  NOT NULL,
                         slug                VARCHAR(220)  NOT NULL,
                         description         VARCHAR(2000),
                         short_description   VARCHAR(500),
                         thumbnail_url       VARCHAR(255),
                         price               DOUBLE PRECISION NOT NULL,
                         currency            VARCHAR(3)    NOT NULL DEFAULT 'XAF',
                         level               VARCHAR(30)   NOT NULL DEFAULT 'BEGINNER',
                         category            VARCHAR(100),
                         instructor_id       UUID,
                         idempotency_key     VARCHAR(255)  NOT NULL,
                         deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
                         duration_in_minutes INTEGER       NOT NULL,
                         enrolled_count      INTEGER       NOT NULL DEFAULT 0,
                         published           BOOLEAN       NOT NULL DEFAULT FALSE,
                         published_at        TIMESTAMP,
                         created_at          TIMESTAMP     NOT NULL DEFAULT now(),
                         updated_at          TIMESTAMP     NOT NULL DEFAULT now(),

                         CONSTRAINT uq_courses_slug            UNIQUE (slug),
                         CONSTRAINT uq_courses_idempotency_key  UNIQUE (idempotency_key),
                         CONSTRAINT fk_courses_instructor
                             FOREIGN KEY (instructor_id) REFERENCES users (id)
                                 ON DELETE SET NULL,
                         CONSTRAINT chk_courses_level
                             CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
                         CONSTRAINT chk_courses_price_non_negative
                             CHECK (price >= 0),
                         CONSTRAINT chk_courses_duration_non_negative
                             CHECK (duration_in_minutes >= 0),
                         CONSTRAINT chk_courses_enrolled_count_non_negative
                             CHECK (enrolled_count >= 0)
);

CREATE INDEX idx_courses_instructor_id ON courses (instructor_id);
CREATE INDEX idx_courses_category      ON courses (category);
CREATE INDEX idx_courses_published     ON courses (published);
CREATE INDEX idx_courses_deleted       ON courses (deleted);

-- ----------------------------------------------------------------------------
-- @ElementCollection: uploadVideos -> course_upload_videos
-- ----------------------------------------------------------------------------
CREATE TABLE course_upload_videos (
                                      course_id  UUID         NOT NULL,
                                      video_url  VARCHAR(500) NOT NULL,

                                      CONSTRAINT fk_course_upload_videos_course
                                          FOREIGN KEY (course_id) REFERENCES courses (id)
                                              ON DELETE CASCADE
);

CREATE INDEX idx_course_upload_videos_course_id ON course_upload_videos (course_id);

-- ----------------------------------------------------------------------------
-- @ElementCollection: uploadDocuments -> course_upload_documents
-- ----------------------------------------------------------------------------
CREATE TABLE course_upload_documents (
                                         course_id     UUID         NOT NULL,
                                         document_url  VARCHAR(500) NOT NULL,

                                         CONSTRAINT fk_course_upload_documents_course
                                             FOREIGN KEY (course_id) REFERENCES courses (id)
                                                 ON DELETE CASCADE
);

CREATE INDEX idx_course_upload_documents_course_id ON course_upload_documents (course_id);

-- ----------------------------------------------------------------------------
-- @ElementCollection: tags -> course_tags
-- ----------------------------------------------------------------------------
CREATE TABLE course_tags (
                             course_id UUID         NOT NULL,
                             tag       VARCHAR(100) NOT NULL,

                             CONSTRAINT fk_course_tags_course
                                 FOREIGN KEY (course_id) REFERENCES courses (id)
                                     ON DELETE CASCADE
);

CREATE INDEX idx_course_tags_course_id ON course_tags (course_id);
CREATE INDEX idx_course_tags_tag       ON course_tags (tag);