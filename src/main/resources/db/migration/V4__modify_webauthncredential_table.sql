ALTER TABLE web_authn_credential
    ADD credential_record JSONB;

ALTER TABLE web_authn_credential
    DROP COLUMN aaguid;

ALTER TABLE web_authn_credential
    DROP COLUMN credential_id;

ALTER TABLE web_authn_credential
    DROP COLUMN public_key_cose;

ALTER TABLE web_authn_credential
    DROP COLUMN sign_count;

ALTER TABLE web_authn_credential
    DROP COLUMN transports;