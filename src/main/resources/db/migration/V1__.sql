CREATE SEQUENCE IF NOT EXISTS section_seq START WITH 1 INCREMENT BY 50;

ALTER TABLE participant
    ADD parent_user_id UUID;

ALTER TABLE participant
    ADD primary_contact_email VARCHAR(255);

ALTER TABLE participant
    ADD section_id BIGINT;

ALTER TABLE participant
    ADD student_user_id UUID;

ALTER TABLE users
    ADD parent_user_id UUID;

ALTER TABLE participant
    ALTER COLUMN section_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_keycloakid UNIQUE (keycloak_id);

ALTER TABLE participant
    ADD CONSTRAINT uk_participant_student_user UNIQUE (student_user_id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_PARENT_USER FOREIGN KEY (parent_user_id) REFERENCES users (id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_SECTION FOREIGN KEY (section_id) REFERENCES section (id);

ALTER TABLE participant
    ADD CONSTRAINT FK_PARTICIPANT_ON_STUDENT_USER FOREIGN KEY (student_user_id) REFERENCES users (id);

ALTER TABLE users
    ADD CONSTRAINT FK_USERS_ON_PARENT_USER FOREIGN KEY (parent_user_id) REFERENCES participant (id);

ALTER TABLE participant
DROP
COLUMN parent1email;

ALTER TABLE participant
DROP
COLUMN parent1nom;

ALTER TABLE participant
DROP
COLUMN parent1prenom;

ALTER TABLE participant
DROP
COLUMN parent1telephone;

ALTER TABLE participant
DROP
COLUMN parent2email;

ALTER TABLE participant
DROP
COLUMN parent2nom;

ALTER TABLE participant
DROP
COLUMN parent2prenom;

ALTER TABLE participant
DROP
COLUMN parent2telephone;

ALTER TABLE participant
DROP
COLUMN section;

ALTER TABLE participant
DROP
COLUMN id;

DROP SEQUENCE participant_seq CASCADE;

ALTER TABLE participant
    ALTER COLUMN email SET NOT NULL;

ALTER TABLE participant
    ADD id UUID NOT NULL PRIMARY KEY;

ALTER TABLE participant
    ADD CONSTRAINT uk_participant_student_user UNIQUE (id);

ALTER TABLE participant
    ALTER COLUMN nom SET NOT NULL;

ALTER TABLE participant_documents
DROP
COLUMN participant_id;

ALTER TABLE participant_documents
    ADD participant_id UUID NOT NULL;

ALTER TABLE participant_documents
    ADD CONSTRAINT fk_pardoc_on_participant FOREIGN KEY (participant_id) REFERENCES participant (id);

ALTER TABLE voyage_participant
DROP
COLUMN participant_id;

ALTER TABLE voyage_participant
    ADD participant_id UUID;

ALTER TABLE voyage_participant
    ADD CONSTRAINT FK_VOYAGEPARTICIPANT_ON_PARTICIPANT FOREIGN KEY (participant_id) REFERENCES participant (id);

ALTER TABLE participant
    ALTER COLUMN prenom SET NOT NULL;

ALTER TABLE participant
    ALTER COLUMN sexe SET NOT NULL;