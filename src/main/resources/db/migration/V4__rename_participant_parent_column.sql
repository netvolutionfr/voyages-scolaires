-- Renommage sémantique : participant.parent_user_id -> legal_guardian_user_id

-- Drop FK existante si nommée différemment : la retrouver dynamiquement
DO $$
    DECLARE
        fk_name text;
    BEGIN
        SELECT conname INTO fk_name
        FROM pg_constraint
        WHERE conrelid = 'participant'::regclass
          AND contype = 'f'
          AND conname LIKE 'fk%parent%user%';

        IF fk_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE participant DROP CONSTRAINT %I', fk_name);
        END IF;
    END$$;

-- Renommer la colonne si elle existe encore
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name='participant' AND column_name='parent_user_id'
        ) THEN
            ALTER TABLE participant RENAME COLUMN parent_user_id TO legal_guardian_user_id;
        END IF;
    END$$;

-- Recréer la FK vers users(id) avec un nom explicite
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_participant_legal_guardian_user'
        ) THEN
            ALTER TABLE participant
                ADD CONSTRAINT fk_participant_legal_guardian_user
                    FOREIGN KEY (legal_guardian_user_id) REFERENCES users(id) ON DELETE SET NULL;
        END IF;
    END$$;

-- Index utile
CREATE INDEX IF NOT EXISTS idx_participant_legal_guardian ON participant(legal_guardian_user_id);
