CREATE TABLE rectification_request
(
    id              UUID                        NOT NULL,
    user_id         BIGINT                      NOT NULL,
    field           VARCHAR(32)                 NOT NULL,
    requested_value VARCHAR(255)                NOT NULL,
    reason          VARCHAR(1000),
    status          VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
    admin_comment   VARCHAR(1000),
    processed_by_id BIGINT,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    processed_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_rectification_request PRIMARY KEY (id)
);

ALTER TABLE rectification_request
    ADD CONSTRAINT FK_RECTIFICATION_REQUEST_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE rectification_request
    ADD CONSTRAINT FK_RECTIFICATION_REQUEST_ON_PROCESSED_BY FOREIGN KEY (processed_by_id) REFERENCES users (id);

CREATE INDEX idx_rectification_request_user ON rectification_request (user_id);

CREATE INDEX idx_rectification_request_status ON rectification_request (status);
