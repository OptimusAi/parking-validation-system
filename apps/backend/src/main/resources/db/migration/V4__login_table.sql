-- ─── login ────────────────────────────────────────────────────────────────────
-- Tracks every identity-provider account that has ever authenticated.
-- One login row per (provider, provider_user_id) pair; linked to the app user.
CREATE TABLE login (
    id                 VARCHAR(36)   NOT NULL,
    login_provider     VARCHAR(128)  NOT NULL,
    provider_user_id   VARCHAR(256)  NOT NULL,
    email              VARCHAR(128),
    email_prefix       VARCHAR(256),
    last_login_date    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    user_id            UUID,
    gtoken             TEXT,
    CONSTRAINT login_pkey PRIMARY KEY (id),
    CONSTRAINT login_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX uq_login_provider_user_id
    ON login (login_provider, provider_user_id);

CREATE INDEX idx_login_email ON login (email);

