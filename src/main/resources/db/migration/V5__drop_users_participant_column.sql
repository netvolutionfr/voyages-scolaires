-- Supprimer le lien ambigu côté users (users.parent_user_id -> participant.id)

-- Drop FK si présente
DO $$
    DECLARE
        fk_name text;
    BEGIN
        SELECT conname INTO fk_name
        FROM pg_constraint
        WHERE conrelid = 'users'::regclass
          AND contype = 'f'
          AND conname LIKE 'fk%parent%user%id%participant%';

        IF fk_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', fk_name);
        END IF;
    END$$;

-- Drop colonne si présente
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name='users' AND column_name='parent_user_id'
        ) THEN
            ALTER TABLE users DROP COLUMN parent_user_id;
        END IF;
    END$$;
