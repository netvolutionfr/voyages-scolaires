CREATE TABLE refresh_tokens
(
    id             BIGINT                       NOT NULL,
    user_id        BIGINT                       NOT NULL,
    token_hash     BYTEA                        NOT NULL,
    family_id      UUID                         NOT NULL,
    issued_at      TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    expires_at     TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    last_used_at   TIMESTAMP WITHOUT TIME ZONE,
    status         VARCHAR(16) DEFAULT 'ACTIVE' NOT NULL,
    replaced_by_id BIGINT,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_replaced_by UNIQUE (replaced_by_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_token_hash UNIQUE (token_hash);

CREATE INDEX idx_refresh_status ON refresh_tokens (status);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_REPLACED_BY FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
