-- 1) Nouvelles colonnes sur voyage
ALTER TABLE voyage
    ADD COLUMN IF NOT EXISTS prix_total INT,
    ADD COLUMN IF NOT EXISTS cover_photo_url TEXT;

-- 2) Collection des secteurs (enum stocké en texte)
--    Clé primaire (voyage_id, secteur) pour éviter les doublons.
CREATE TABLE IF NOT EXISTS voyage_secteurs (
                                               voyage_id BIGINT NOT NULL,
                                               secteur   VARCHAR(32) NOT NULL,
                                               CONSTRAINT pk_voyage_secteurs PRIMARY KEY (voyage_id, secteur),
                                               CONSTRAINT fk_voyage_secteurs_voyage FOREIGN KEY (voyage_id) REFERENCES voyage(id) ON DELETE CASCADE
);
