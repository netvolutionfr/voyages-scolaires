ALTER TABLE webauthn_credentials
    DROP CONSTRAINT webauthn_credentials_user_id_fkey;

CREATE TABLE registration_attempt
(
    id         BIGINT  NOT NULL,
    uuid       UUID,
    challenge  BYTEA,
    email_hint VARCHAR(255),
    rp_id      VARCHAR(255),
    origin     VARCHAR(255),
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    used       BOOLEAN NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_registrationattempt PRIMARY KEY (id)
);

CREATE TABLE web_authn_credential
(
    id              BIGINT       NOT NULL,
    user_id         BIGINT,
    credential_id   VARCHAR(255) NOT NULL,
    public_key_cose BYTEA        NOT NULL,
    sign_count      BIGINT       NOT NULL,
    aaguid          VARCHAR(255),
    transports      VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_webauthncredential PRIMARY KEY (id)
);

ALTER TABLE users
    ADD status VARCHAR(255);

ALTER TABLE registration_attempt
    ADD CONSTRAINT uc_registrationattempt_uuid UNIQUE (uuid);

ALTER TABLE web_authn_credential
    ADD CONSTRAINT uc_webauthncredential_credentialid UNIQUE (credential_id);

ALTER TABLE web_authn_credential
    ADD CONSTRAINT FK_WEBAUTHNCREDENTIAL_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

DROP TABLE webauthn_credentials CASCADE;

DROP TABLE webauthn_user_entity CASCADE;

ALTER TABLE section
    DROP COLUMN created_at;

ALTER TABLE section
    ALTER COLUMN cycle SET NOT NULL;

ALTER TABLE section
    ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE section
    ALTER COLUMN year SET NOT NULL;