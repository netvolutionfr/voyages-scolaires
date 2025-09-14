ALTER TABLE section
    ADD public_id UUID DEFAULT gen_random_uuid();

ALTER TABLE section
    ALTER COLUMN public_id SET NOT NULL;

ALTER TABLE section
    ADD CONSTRAINT uc_section_publicid UNIQUE (public_id);

ALTER TABLE parent_child
    DROP COLUMN created_at;
