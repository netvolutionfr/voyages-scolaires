-- Rendre participant.student_user_id obligatoire (+ FK explicite)
-- D'abord, garde-fou : bloquer la migration si des lignes nulles existent

DO $$
    DECLARE
        cnt bigint;
    BEGIN
        SELECT COUNT(*) INTO cnt FROM participant WHERE student_user_id IS NULL;
        IF cnt > 0 THEN
            RAISE EXCEPTION 'Migration bloquée: % participant(s) sans student_user_id. Corrigez les données avant de continuer.', cnt;
        END IF;
    END$$;

-- Ajouter la contrainte NOT NULL
ALTER TABLE participant
    ALTER COLUMN student_user_id SET NOT NULL;

-- S'assurer de la contrainte d'unicité (tu as déjà uk_participant_student_user)
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'uk_participant_student_user'
        ) THEN
            ALTER TABLE participant
                ADD CONSTRAINT uk_participant_student_user UNIQUE (student_user_id);
        END IF;
    END$$;

-- FK explicite vers users(id) si absente
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_participant_student_user'
        ) THEN
            ALTER TABLE participant
                ADD CONSTRAINT fk_participant_student_user
                    FOREIGN KEY (student_user_id) REFERENCES users(id) ON DELETE RESTRICT;
        END IF;
    END$$;

-- Index (la unique crée déjà un index, donc pas nécessaire ici)
