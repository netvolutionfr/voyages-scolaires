-- Pays de transit (escales) d'un voyage. Trip.country reste le pays de destination.
CREATE TABLE trip_transit_countries
(
    trip_id    BIGINT NOT NULL,
    country_id BIGINT NOT NULL,
    CONSTRAINT pk_trip_transit_countries PRIMARY KEY (trip_id, country_id),
    CONSTRAINT fk_trip_transit_countries_on_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_transit_countries_on_country FOREIGN KEY (country_id) REFERENCES country (id)
);

-- Une formalité de pays s'applique aussi quand ce pays n'est traversé qu'en transit.
ALTER TABLE country_formality_templates
    ADD COLUMN applies_in_transit BOOLEAN NOT NULL DEFAULT FALSE;
-- Valeurs initiales : voir R__06_transit_formalities.sql (exécuté après les seeds R__04).
