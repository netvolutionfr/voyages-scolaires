-- Table pour les "user entities"
create table webauthn_user_entity (
                                      id bytea primary key,                -- user handle (stable, binaire)
                                      username varchar(255) not null,      -- identifiant lisible (email ou login)
                                      display_name varchar(255) not null   -- affichage (nom complet, prénom, etc.)
);

-- Table pour les credentials liés à un user
create table webauthn_credentials (
                                      id bytea primary key,                -- credentialId
                                      user_id bytea not null references webauthn_user_entity(id) on delete cascade,
                                      public_key bytea not null,
                                      signature_count bigint not null,
                                      aaguid uuid,
                                      attestation_format varchar(255),
                                      transports varchar(255),
                                      backup_eligible boolean,
                                      backup_state boolean
);