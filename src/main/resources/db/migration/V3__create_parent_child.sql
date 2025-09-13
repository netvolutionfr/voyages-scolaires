-- 1) Table de relation parent ↔ enfant (User parent / Participant enfant)

CREATE TABLE IF NOT EXISTS parent_child (
                                            id BIGSERIAL PRIMARY KEY,
                                            parent_id BIGINT NOT NULL,
                                            child_id  BIGINT NOT NULL,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Un parent ne peut être lié qu'une fois à un même enfant
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'uk_parent_child_parent_child'
        ) THEN
            ALTER TABLE parent_child
                ADD CONSTRAINT uk_parent_child_parent_child UNIQUE (parent_id, child_id);
        END IF;
    END$$;

-- FKs
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_parent_child_parent'
        ) THEN
            ALTER TABLE parent_child
                ADD CONSTRAINT fk_parent_child_parent
                    FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_parent_child_child'
        ) THEN
            ALTER TABLE parent_child
                ADD CONSTRAINT fk_parent_child_child
                    FOREIGN KEY (child_id) REFERENCES participant(id) ON DELETE CASCADE;
        END IF;
    END$$;

-- Index utiles
CREATE INDEX IF NOT EXISTS idx_parent_child_parent ON parent_child(parent_id);
CREATE INDEX IF NOT EXISTS idx_parent_child_child  ON parent_child(child_id);
