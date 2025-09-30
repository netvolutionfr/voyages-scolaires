CREATE SEQUENCE IF NOT EXISTS cust_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE country
(
    id   BIGINT NOT NULL,
    name VARCHAR(255),
    CONSTRAINT pk_country PRIMARY KEY (id)
);

CREATE TABLE country_formality_templates
(
    id                        BIGINT      NOT NULL,
    country_id                BIGINT      NOT NULL,
    document_type_id          BIGINT      NOT NULL,
    type                      VARCHAR(16) NOT NULL,
    required                  BOOLEAN     NOT NULL,
    days_before_trip          INTEGER,
    accepted_mime             VARCHAR(1024),
    max_size_mb               INTEGER,
    days_retention_after_trip INTEGER,
    store_scan                BOOLEAN,
    trip_condition            JSONB,
    notes                     VARCHAR(512),
    CONSTRAINT pk_country_formality_templates PRIMARY KEY (id)
);

CREATE TABLE document
(
    id                  BIGINT NOT NULL,
    file_name           VARCHAR(255),
    file_type           VARCHAR(255),
    file_size           BIGINT,
    file_url            VARCHAR(255),
    document_type_id    BIGINT NOT NULL,
    document_status     VARCHAR(255),
    file_number         VARCHAR(255),
    delivery_date       date,
    expiration_date     date,
    trip_participant_id BIGINT NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE,
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_document PRIMARY KEY (id)
);

CREATE TABLE document_type
(
    id          BIGINT NOT NULL,
    abr         VARCHAR(255),
    label       VARCHAR(255),
    description VARCHAR(255),
    CONSTRAINT pk_documenttype PRIMARY KEY (id)
);

CREATE TABLE parent_child
(
    id        BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    child_id  BIGINT NOT NULL,
    CONSTRAINT pk_parentchild PRIMARY KEY (id)
);

CREATE TABLE participant
(
    id                     BIGINT       NOT NULL,
    public_id              UUID         NOT NULL,
    last_name              VARCHAR(255) NOT NULL,
    first_name             VARCHAR(255) NOT NULL,
    gender                 VARCHAR(255) NOT NULL,
    email                  VARCHAR(255) NOT NULL,
    telephone              VARCHAR(255),
    birth_date             TEXT,
    section_id             BIGINT       NOT NULL,
    student_user_id        BIGINT       NOT NULL,
    legal_guardian_user_id BIGINT,
    primary_contact_email  VARCHAR(255),
    created_at             TIMESTAMP WITHOUT TIME ZONE,
    updated_at             TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_participant PRIMARY KEY (id)
);

CREATE TABLE participant_documents
(
    participant_id BIGINT NOT NULL,
    documents_id   BIGINT NOT NULL
);

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
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_registrationattempt PRIMARY KEY (id)
);

CREATE TABLE section
(
    id          BIGINT      NOT NULL,
    public_id   UUID        NOT NULL,
    label       VARCHAR(255),
    description VARCHAR(255),
    cycle       VARCHAR(32) NOT NULL,
    year        VARCHAR(16) NOT NULL,
    is_active   BOOLEAN     NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_section PRIMARY KEY (id)
);

CREATE TABLE trip
(
    id                        BIGINT NOT NULL,
    public_id                 UUID   NOT NULL,
    title                     VARCHAR(255),
    description               VARCHAR(255),
    destination               VARCHAR(255),
    total_price               INTEGER,
    family_contribution       INTEGER,
    country_id                BIGINT,
    departure_date            date,
    return_date               date,
    min_participants          INTEGER,
    max_participants          INTEGER,
    registration_opening_date date,
    registration_closing_date date,
    poll                      BOOLEAN,
    cover_photo_url           VARCHAR(255),
    created_at                TIMESTAMP WITHOUT TIME ZONE,
    updated_at                TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_trip PRIMARY KEY (id)
);

CREATE TABLE trip_chaperones
(
    trip_id       BIGINT NOT NULL,
    chaperones_id BIGINT NOT NULL
);

CREATE TABLE trip_formality
(
    id                        BIGINT      NOT NULL,
    trip_id                   BIGINT      NOT NULL,
    source_template_id        BIGINT,
    document_type_id          BIGINT      NOT NULL,
    formality_type            VARCHAR(16) NOT NULL,
    required                  BOOLEAN     NOT NULL,
    days_before_trip          INTEGER,
    accepted_mime             VARCHAR(1024),
    max_size_mb               INTEGER,
    days_retention_after_trip INTEGER,
    store_scan                BOOLEAN,
    trip_condition            JSONB,
    notes                     VARCHAR(512),
    manually_added            BOOLEAN,
    CONSTRAINT pk_trip_formality PRIMARY KEY (id)
);

CREATE TABLE trip_mandatory_document_types
(
    trip_id                     BIGINT NOT NULL,
    mandatory_document_types_id BIGINT NOT NULL
);

CREATE TABLE trip_participant
(
    id                  BIGINT  NOT NULL,
    chaperone           BOOLEAN NOT NULL,
    registration_date   TIMESTAMP WITHOUT TIME ZONE,
    decision_date       TIMESTAMP WITHOUT TIME ZONE,
    registration_status SMALLINT,
    decision_message    VARCHAR(255),
    admin_notes         VARCHAR(255),
    trip_id             BIGINT,
    participant_id      BIGINT,
    CONSTRAINT pk_tripparticipant PRIMARY KEY (id)
);

CREATE TABLE trip_participants
(
    trip_id         BIGINT NOT NULL,
    participants_id BIGINT NOT NULL
);

CREATE TABLE trip_preferences
(
    id       BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    trip_id  BIGINT                                  NOT NULL,
    user_id  BIGINT                                  NOT NULL,
    interest VARCHAR(255)                            NOT NULL,
    CONSTRAINT pk_trip_preferences PRIMARY KEY (id)
);

CREATE TABLE trip_sections
(
    trip_id     BIGINT NOT NULL,
    sections_id BIGINT NOT NULL
);

CREATE TABLE trip_sectors
(
    trip_id BIGINT NOT NULL,
    sector  VARCHAR(255)
);

CREATE TABLE users
(
    id               BIGINT NOT NULL,
    public_id        UUID   NOT NULL,
    keycloak_id      VARCHAR(255),
    email            VARCHAR(255),
    last_name        VARCHAR(255),
    first_name       VARCHAR(255),
    telephone        VARCHAR(255),
    role             VARCHAR(255),
    status           VARCHAR(255),
    consent_given_at date,
    consent_text     VARCHAR(255),
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE web_authn_credential
(
    id              BIGINT NOT NULL,
    user_id         BIGINT,
    credential_id   BYTEA,
    public_key      BYTEA,
    signature_count BIGINT NOT NULL,
    user_handle     BYTEA,
    aaguid          VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_webauthncredential PRIMARY KEY (id)
);

ALTER TABLE trip_preferences
    ADD CONSTRAINT uc_37797cf027ed8f998f5ac6b72 UNIQUE (trip_id, user_id);

ALTER TABLE parent_child
    ADD CONSTRAINT uc_d8c42c6b975ff4507ae9dbd9f UNIQUE (parent_id, child_id);

ALTER TABLE participant_documents
    ADD CONSTRAINT uc_participant_documents_documents UNIQUE (documents_id);

ALTER TABLE participant
    ADD CONSTRAINT uc_participant_publicid UNIQUE (public_id);

ALTER TABLE registration_attempt
    ADD CONSTRAINT uc_registrationattempt_uuid UNIQUE (uuid);

ALTER TABLE section
    ADD CONSTRAINT uc_section_publicid UNIQUE (public_id);

ALTER TABLE trip
    ADD CONSTRAINT uc_trip_publicid UNIQUE (public_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_keycloakid UNIQUE (keycloak_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_publicid UNIQUE (public_id);

ALTER TABLE participant
    ADD CONSTRAINT uk_participant_student_user UNIQUE (student_user_id);

ALTER TABLE section
    ADD CONSTRAINT ux_section_label UNIQUE (label);

CREATE INDEX ix_section_active ON section (is_active);

CREATE INDEX ix_section_cycle ON section (cycle);

CREATE INDEX ix_section_year ON section (year);

ALTER TABLE country_formality_templates
    ADD CONSTRAINT FK_COUNTRY_FORMALITY_TEMPLATES_ON_COUNTRY FOREIGN KEY (country_id) REFERENCES country (id);

CREATE INDEX idx_fpt_country ON country_formality_templates (country_id);

ALTER TABLE country_formality_templates
    ADD CONSTRAINT FK_COUNTRY_FORMALITY_TEMPLATES_ON_DOCUMENT_TYPE FOREIGN KEY (document_type_id) REFERENCES document_type (id);

CREATE INDEX idx_fpt_doc_type ON country_formality_templates (document_type_id);

ALTER TABLE document
    ADD CONSTRAINT FK_DOCUMENT_ON_DOCUMENTTYPE FOREIGN KEY (document_type_id) REFERENCES document_type (id);

ALTER TABLE document
    ADD CONSTRAINT FK_DOCUMENT_ON_TRIPPARTICIPANT FOREIGN KEY (trip_participant_id) REFERENCES trip_participant (id);

ALTER TABLE parent_child
    ADD CONSTRAINT FK_PARENTCHILD_ON_CHILD FOREIGN KEY (child_id) REFERENCES participant (id);

ALTER TABLE parent_child
    ADD CONSTRAINT FK_PARENTCHILD_ON_PARENT FOREIGN KEY (parent_id) REFERENCES users (id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_LEGAL_GUARDIAN_USER FOREIGN KEY (legal_guardian_user_id) REFERENCES users (id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_SECTION FOREIGN KEY (section_id) REFERENCES section (id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_STUDENT_USER FOREIGN KEY (student_user_id) REFERENCES users (id);

ALTER TABLE trip_participant
    ADD CONSTRAINT FK_TRIPPARTICIPANT_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participant (id);

ALTER TABLE trip_participant
    ADD CONSTRAINT FK_TRIPPARTICIPANT_ON_TRIP FOREIGN KEY (trip_id) REFERENCES trip (id);

ALTER TABLE trip_formality
    ADD CONSTRAINT FK_TRIP_FORMALITY_ON_DOCUMENT_TYPE FOREIGN KEY (document_type_id) REFERENCES document_type (id);

CREATE INDEX idx_fv_doc_type ON trip_formality (document_type_id);

ALTER TABLE trip_formality
    ADD CONSTRAINT FK_TRIP_FORMALITY_ON_SOURCE_TEMPLATE FOREIGN KEY (source_template_id) REFERENCES country_formality_templates (id);

ALTER TABLE trip_formality
    ADD CONSTRAINT FK_TRIP_FORMALITY_ON_TRIP FOREIGN KEY (trip_id) REFERENCES trip (id);

CREATE INDEX idx_fv_trip ON trip_formality (trip_id);

ALTER TABLE trip
    ADD CONSTRAINT FK_TRIP_ON_COUNTRY FOREIGN KEY (country_id) REFERENCES country (id);

ALTER TABLE trip_preferences
    ADD CONSTRAINT FK_TRIP_PREFERENCES_ON_TRIP FOREIGN KEY (trip_id) REFERENCES trip (id);

ALTER TABLE trip_preferences
    ADD CONSTRAINT FK_TRIP_PREFERENCES_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE web_authn_credential
    ADD CONSTRAINT FK_WEBAUTHNCREDENTIAL_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE participant_documents
    ADD CONSTRAINT fk_pardoc_on_document FOREIGN KEY (documents_id) REFERENCES document (id);

ALTER TABLE participant_documents
    ADD CONSTRAINT fk_pardoc_on_participant FOREIGN KEY (participant_id) REFERENCES participant (id);

ALTER TABLE trip_chaperones
    ADD CONSTRAINT fk_tricha_on_trip FOREIGN KEY (trip_id) REFERENCES trip (id);

ALTER TABLE trip_chaperones
    ADD CONSTRAINT fk_tricha_on_user FOREIGN KEY (chaperones_id) REFERENCES users (id);

ALTER TABLE trip_mandatory_document_types
    ADD CONSTRAINT fk_trimandoctyp_on_document_type FOREIGN KEY (mandatory_document_types_id) REFERENCES document_type (id);

ALTER TABLE trip_mandatory_document_types
    ADD CONSTRAINT fk_trimandoctyp_on_trip FOREIGN KEY (trip_id) REFERENCES trip (id);

ALTER TABLE trip_sectors
    ADD CONSTRAINT fk_trip_sectors_on_trip FOREIGN KEY (trip_id) REFERENCES trip (id);

ALTER TABLE trip_participants
    ADD CONSTRAINT fk_tripar_on_trip FOREIGN KEY (trip_id) REFERENCES trip (id);

ALTER TABLE trip_participants
    ADD CONSTRAINT fk_tripar_on_trip_participant FOREIGN KEY (participants_id) REFERENCES trip_participant (id);

ALTER TABLE trip_sections
    ADD CONSTRAINT fk_trisec_on_section FOREIGN KEY (sections_id) REFERENCES section (id);

ALTER TABLE trip_sections
    ADD CONSTRAINT fk_trisec_on_trip FOREIGN KEY (trip_id) REFERENCES trip (id);