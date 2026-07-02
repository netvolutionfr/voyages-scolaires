-- Effacement RGPD (Art. 17) — voir docs/adr/0003-effacement-par-suppression-totale.md
--
-- Cascade DB uniquement sur les tables purement techniques, où une suppression
-- silencieuse ne masque aucun oubli métier. Documents, fiche santé, liens
-- familiaux, inscriptions voyages et demandes de rectification restent
-- SANS cascade : leur suppression est explicite dans UserErasureService.

ALTER TABLE otp_tokens
    DROP CONSTRAINT FK_OTP_TOKENS_ON_USER;
ALTER TABLE otp_tokens
    ADD CONSTRAINT FK_OTP_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    DROP CONSTRAINT FK_REFRESH_TOKENS_ON_USER;
ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    DROP CONSTRAINT FK_REFRESH_TOKENS_ON_REPLACED_BY;
ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_REPLACED_BY FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id) ON DELETE SET NULL;

ALTER TABLE web_authn_credential
    DROP CONSTRAINT FK_WEBAUTHNCREDENTIAL_ON_USER;
ALTER TABLE web_authn_credential
    ADD CONSTRAINT FK_WEBAUTHNCREDENTIAL_ON_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE users
    DROP CONSTRAINT FK_USERS_ON_LEGAL_GUARDIAN_USER;
ALTER TABLE users
    ADD CONSTRAINT FK_USERS_ON_LEGAL_GUARDIAN_USER FOREIGN KEY (legal_guardian_user_id) REFERENCES users (id) ON DELETE SET NULL;
