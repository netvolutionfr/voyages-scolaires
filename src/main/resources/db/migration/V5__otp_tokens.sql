CREATE SEQUENCE IF NOT EXISTS otp_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE otp_tokens
(
    id          BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    purpose     VARCHAR(40)  NOT NULL,
    code_hash   VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    attempts    INTEGER      NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_otp_tokens PRIMARY KEY (id)
);

CREATE INDEX idx_otp_expires_at ON otp_tokens (expires_at);

CREATE INDEX idx_otp_user_status ON otp_tokens (user_id, status);

ALTER TABLE otp_tokens
    ADD CONSTRAINT FK_OTP_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);