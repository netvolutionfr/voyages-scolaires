CREATE SEQUENCE IF NOT EXISTS cust_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE document
(
    id                    BIGINT NOT NULL,
    fichier_nom           VARCHAR(255),
    fichier_type          VARCHAR(255),
    fichier_taille        BIGINT,
    fichier_url           VARCHAR(255),
    type_document_id      BIGINT NOT NULL,
    etat_document         VARCHAR(255),
    numero                VARCHAR(255),
    date_emission         date,
    date_expiration       date,
    voyage_participant_id BIGINT NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE,
    updated_at            TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_document PRIMARY KEY (id)
);

CREATE TABLE formalites_pays_templates
(
    id                              BIGINT      NOT NULL,
    pays_id                         BIGINT      NOT NULL,
    type_document_id                BIGINT      NOT NULL,
    type                            VARCHAR(16) NOT NULL,
    required                        BOOLEAN     NOT NULL,
    delai_fourniture_avant_depart   INTEGER,
    accepted_mime                   VARCHAR(1024),
    max_size_mb                     INTEGER,
    delai_conservation_apres_voyage INTEGER,
    store_scan                      BOOLEAN,
    trip_condition                  JSONB,
    notes                           VARCHAR(512),
    CONSTRAINT pk_formalites_pays_templates PRIMARY KEY (id)
);

CREATE TABLE formalites_voyage
(
    id                              BIGINT      NOT NULL,
    voyage_id                       BIGINT      NOT NULL,
    source_template_id              BIGINT,
    type_document_id                BIGINT      NOT NULL,
    type                            VARCHAR(16) NOT NULL,
    required                        BOOLEAN     NOT NULL,
    delai_fourniture_avant_depart   INTEGER,
    accepted_mime                   VARCHAR(1024),
    max_size_mb                     INTEGER,
    delai_conservation_apres_voyage INTEGER,
    store_scan                      BOOLEAN,
    trip_condition                  JSONB,
    notes                           VARCHAR(512),
    manually_added                  BOOLEAN,
    CONSTRAINT pk_formalites_voyage PRIMARY KEY (id)
);

CREATE TABLE participant
(
    id                    BIGINT       NOT NULL,
    public_id             UUID         NOT NULL,
    nom                   VARCHAR(255) NOT NULL,
    prenom                VARCHAR(255) NOT NULL,
    sexe                  VARCHAR(255) NOT NULL,
    email                 VARCHAR(255) NOT NULL,
    telephone             VARCHAR(255),
    date_naissance        TEXT,
    section_id            BIGINT       NOT NULL,
    parent_user_id        BIGINT,
    student_user_id       BIGINT,
    primary_contact_email VARCHAR(255),
    created_at            TIMESTAMP WITHOUT TIME ZONE,
    updated_at            TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_participant PRIMARY KEY (id)
);

CREATE TABLE participant_documents
(
    participant_id BIGINT NOT NULL,
    documents_id   BIGINT NOT NULL
);

CREATE TABLE pays
(
    id  BIGINT NOT NULL,
    nom VARCHAR(255),
    CONSTRAINT pk_pays PRIMARY KEY (id)
);

CREATE TABLE section
(
    id          BIGINT NOT NULL,
    libelle     VARCHAR(255),
    description VARCHAR(255),
    CONSTRAINT pk_section PRIMARY KEY (id)
);

CREATE TABLE type_document
(
    id          BIGINT NOT NULL,
    abr         VARCHAR(255),
    nom         VARCHAR(255),
    description VARCHAR(255),
    CONSTRAINT pk_typedocument PRIMARY KEY (id)
);

CREATE TABLE users
(
    id               BIGINT NOT NULL,
    public_id        UUID   NOT NULL,
    keycloak_id      VARCHAR(255),
    email            VARCHAR(255),
    nom              VARCHAR(255),
    prenom           VARCHAR(255),
    telephone        VARCHAR(255),
    parent_user_id   BIGINT,
    role             VARCHAR(255),
    consent_given_at date,
    consent_text     VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE voyage
(
    id                      BIGINT NOT NULL,
    public_id               UUID   NOT NULL,
    nom                     VARCHAR(255),
    description             VARCHAR(255),
    destination             VARCHAR(255),
    pays_id                 BIGINT,
    date_depart             date,
    date_retour             date,
    nombre_min_participants INTEGER,
    nombre_max_participants INTEGER,
    date_debut_inscription  date,
    date_fin_inscription    date,
    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_voyage PRIMARY KEY (id)
);

CREATE TABLE voyage_documents_obligatoires
(
    voyage_id                 BIGINT NOT NULL,
    documents_obligatoires_id BIGINT NOT NULL
);

CREATE TABLE voyage_organisateurs
(
    voyage_id        BIGINT NOT NULL,
    organisateurs_id BIGINT NOT NULL
);

CREATE TABLE voyage_participant
(
    id                   BIGINT  NOT NULL,
    accompagnateur       BOOLEAN NOT NULL,
    organisateur         BOOLEAN NOT NULL,
    date_inscription     TIMESTAMP WITHOUT TIME ZONE,
    date_engagement      TIMESTAMP WITHOUT TIME ZONE,
    statut_inscription   SMALLINT,
    commentaire_decision VARCHAR(255),
    message_motivation   VARCHAR(255),
    voyage_id            BIGINT,
    participant_id       BIGINT,
    CONSTRAINT pk_voyageparticipant PRIMARY KEY (id)
);

CREATE TABLE voyage_participants
(
    voyage_id       BIGINT NOT NULL,
    participants_id BIGINT NOT NULL
);

CREATE TABLE voyage_sections
(
    voyage_id   BIGINT NOT NULL,
    sections_id BIGINT NOT NULL
);

ALTER TABLE participant_documents
    ADD CONSTRAINT uc_participant_documents_documents UNIQUE (documents_id);

ALTER TABLE participant
    ADD CONSTRAINT uc_participant_publicid UNIQUE (public_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_keycloakid UNIQUE (keycloak_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_publicid UNIQUE (public_id);

ALTER TABLE voyage
    ADD CONSTRAINT uc_voyage_publicid UNIQUE (public_id);

ALTER TABLE participant
    ADD CONSTRAINT uk_participant_student_user UNIQUE (student_user_id);

ALTER TABLE document
    ADD CONSTRAINT FK_DOCUMENT_ON_TYPEDOCUMENT FOREIGN KEY (type_document_id) REFERENCES type_document (id);

ALTER TABLE document
    ADD CONSTRAINT FK_DOCUMENT_ON_VOYAGEPARTICIPANT FOREIGN KEY (voyage_participant_id) REFERENCES voyage_participant (id);

ALTER TABLE formalites_pays_templates
    ADD CONSTRAINT FK_FORMALITES_PAYS_TEMPLATES_ON_PAYS FOREIGN KEY (pays_id) REFERENCES pays (id);

CREATE INDEX idx_fpt_country ON formalites_pays_templates (pays_id);

ALTER TABLE formalites_pays_templates
    ADD CONSTRAINT FK_FORMALITES_PAYS_TEMPLATES_ON_TYPE_DOCUMENT FOREIGN KEY (type_document_id) REFERENCES type_document (id);

CREATE INDEX idx_fpt_doc_type ON formalites_pays_templates (type_document_id);

ALTER TABLE formalites_voyage
    ADD CONSTRAINT FK_FORMALITES_VOYAGE_ON_SOURCE_TEMPLATE FOREIGN KEY (source_template_id) REFERENCES formalites_pays_templates (id);

ALTER TABLE formalites_voyage
    ADD CONSTRAINT FK_FORMALITES_VOYAGE_ON_TYPE_DOCUMENT FOREIGN KEY (type_document_id) REFERENCES type_document (id);

CREATE INDEX idx_fv_doc_type ON formalites_voyage (type_document_id);

ALTER TABLE formalites_voyage
    ADD CONSTRAINT FK_FORMALITES_VOYAGE_ON_VOYAGE FOREIGN KEY (voyage_id) REFERENCES voyage (id);

CREATE INDEX idx_fv_voyage ON formalites_voyage (voyage_id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_PARENT_USER FOREIGN KEY (parent_user_id) REFERENCES users (id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_SECTION FOREIGN KEY (section_id) REFERENCES section (id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_STUDENT_USER FOREIGN KEY (student_user_id) REFERENCES users (id);

ALTER TABLE users
    ADD CONSTRAINT FK_USERS_ON_PARENT_USER FOREIGN KEY (parent_user_id) REFERENCES participant (id);

ALTER TABLE voyage_participant
    ADD CONSTRAINT FK_VOYAGEPARTICIPANT_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participant (id);

ALTER TABLE voyage_participant
    ADD CONSTRAINT FK_VOYAGEPARTICIPANT_ON_VOYAGE FOREIGN KEY (voyage_id) REFERENCES voyage (id);

ALTER TABLE voyage
    ADD CONSTRAINT FK_VOYAGE_ON_PAYS FOREIGN KEY (pays_id) REFERENCES pays (id);

ALTER TABLE participant_documents
    ADD CONSTRAINT fk_pardoc_on_document FOREIGN KEY (documents_id) REFERENCES document (id);

ALTER TABLE participant_documents
    ADD CONSTRAINT fk_pardoc_on_participant FOREIGN KEY (participant_id) REFERENCES participant (id);

ALTER TABLE voyage_documents_obligatoires
    ADD CONSTRAINT fk_voydocobl_on_type_document FOREIGN KEY (documents_obligatoires_id) REFERENCES type_document (id);

ALTER TABLE voyage_documents_obligatoires
    ADD CONSTRAINT fk_voydocobl_on_voyage FOREIGN KEY (voyage_id) REFERENCES voyage (id);

ALTER TABLE voyage_organisateurs
    ADD CONSTRAINT fk_voyorg_on_user FOREIGN KEY (organisateurs_id) REFERENCES users (id);

ALTER TABLE voyage_organisateurs
    ADD CONSTRAINT fk_voyorg_on_voyage FOREIGN KEY (voyage_id) REFERENCES voyage (id);

ALTER TABLE voyage_participants
    ADD CONSTRAINT fk_voypar_on_voyage FOREIGN KEY (voyage_id) REFERENCES voyage (id);

ALTER TABLE voyage_participants
    ADD CONSTRAINT fk_voypar_on_voyage_participant FOREIGN KEY (participants_id) REFERENCES voyage_participant (id);

ALTER TABLE voyage_sections
    ADD CONSTRAINT fk_voysec_on_section FOREIGN KEY (sections_id) REFERENCES section (id);

ALTER TABLE voyage_sections
    ADD CONSTRAINT fk_voysec_on_voyage FOREIGN KEY (voyage_id) REFERENCES voyage (id);