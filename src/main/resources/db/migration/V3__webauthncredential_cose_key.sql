ALTER TABLE web_authn_credential
    ADD cose_key BYTEA;

ALTER TABLE web_authn_credential
    ALTER COLUMN cose_key SET NOT NULL;

ALTER TABLE web_authn_credential
DROP
COLUMN public_key;