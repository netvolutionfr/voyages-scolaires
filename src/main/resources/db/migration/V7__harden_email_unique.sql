DO $$
    DECLARE
        dup_count bigint;
    BEGIN
        SELECT COUNT(*) INTO dup_count
        FROM (
                 SELECT email FROM users WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*) > 1
             ) t;

        IF dup_count > 0 THEN
            RAISE EXCEPTION 'Migration bloquée: emails en double détectés dans users (%). Corrigez avant de poser UNIQUE.', dup_count;
        END IF;
    END$$;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename='users' AND indexname='uk_users_email'
        ) THEN
            ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
        END IF;
    END$$;
