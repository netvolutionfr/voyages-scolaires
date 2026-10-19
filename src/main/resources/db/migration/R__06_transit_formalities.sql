-- Exigés même pour une simple escale : passeport + ESTA (États-Unis), passeport + eTA (Canada).
-- Royaume-Uni : transit aérien sans passage frontière exempté d'ETA, donc non marqué.
UPDATE country_formality_templates
SET applies_in_transit = TRUE
WHERE (country_id = 14 AND document_type_id IN (10, 11))
   OR (country_id = 7 AND document_type_id IN (3, 10));
