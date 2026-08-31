CREATE TABLE IF NOT EXISTS users (
                                     id                        UUID PRIMARY KEY,
                                     first_name                VARCHAR(100),
    last_name                 VARCHAR(100),
    email                     VARCHAR(150)      NOT NULL,
    password                  VARCHAR(255)      NOT NULL,
    role                      VARCHAR(50)       NOT NULL DEFAULT 'BURSAR',

    account_verified          BOOLEAN           NOT NULL DEFAULT FALSE,
    account_blocked           BOOLEAN           NOT NULL DEFAULT FALSE,
    account_deleted           BOOLEAN           NOT NULL DEFAULT FALSE,
    account_suspended         BOOLEAN           NOT NULL DEFAULT FALSE,
    account_locked            BOOLEAN           NOT NULL DEFAULT FALSE,

    otp_code                  VARCHAR(20),
    otp_code_verified         BOOLEAN           NOT NULL DEFAULT FALSE,
    otp_expiry_date           TIMESTAMPTZ,

    magic_link_token          VARCHAR(255),

    magic_link_expiry_date    TIMESTAMPTZ,
    verify_magic_link_token   VARCHAR(255),

    forgot_password           VARCHAR(255),
    reset_password            VARCHAR(255),

    failed_login_attempts     INTEGER,
    locked_until              TIMESTAMPTZ,

    access_token              TEXT,
    refresh_token             TEXT,

    created_at                TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ       NOT NULL DEFAULT now()
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email ON users (email);