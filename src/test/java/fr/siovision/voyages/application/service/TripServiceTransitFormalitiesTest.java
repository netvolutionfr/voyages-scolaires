package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripServiceTransitFormalitiesTest {

    private static final long PASSPORT = 10, ESTA = 11, CEAM = 4, HEALTH_FORM = 7;

    @Test
    void transitAddsOnlyTransitFormalitiesNotCoveredByDestination() {
        Country uk = country(34, template(1, HEALTH_FORM, false));
        Country us = country(14,
                template(2, HEALTH_FORM, false),
                template(3, PASSPORT, true),
                template(4, ESTA, true));

        Trip trip = trip(uk, us);
        TripService.syncFormalitiesFromTemplates(trip);

        assertThat(documentTypeIds(trip)).containsExactlyInAnyOrder(HEALTH_FORM, PASSPORT, ESTA);
        // La fiche sanitaire vient de la destination, pas de l'escale.
        assertThat(sourceTemplateIds(trip)).containsExactlyInAnyOrder(1L, 3L, 4L);
    }

    @Test
    void transitIgnoresTemplatesNotFlaggedForTransit() {
        Country usa = country(14, template(1, ESTA, false));
        Country germany = country(2, template(2, CEAM, false));

        Trip trip = trip(usa, germany);
        TripService.syncFormalitiesFromTemplates(trip);

        assertThat(documentTypeIds(trip)).containsExactly(ESTA);
    }

    @Test
    void resyncDropsFormalitiesOfRemovedCountryAndKeepsManualOnes() {
        Country uk = country(34, template(1, HEALTH_FORM, false));
        Country us = country(14, template(2, ESTA, true));
        Trip trip = trip(uk, us);
        TripService.syncFormalitiesFromTemplates(trip);

        TripFormality manual = new TripFormality();
        manual.setDocumentType(documentType(CEAM));
        manual.setManuallyAdded(true);
        trip.addFormality(manual);

        trip.setTransitCountries(List.of());
        TripService.syncFormalitiesFromTemplates(trip);

        assertThat(documentTypeIds(trip)).containsExactlyInAnyOrder(HEALTH_FORM, CEAM);
    }

    @Test
    void resyncKeepsExistingFormalityInstances() {
        Country uk = country(34, template(1, HEALTH_FORM, false));
        Trip trip = trip(uk);
        TripService.syncFormalitiesFromTemplates(trip);
        TripFormality before = trip.getFormalities().getFirst();

        TripService.syncFormalitiesFromTemplates(trip);

        assertThat(trip.getFormalities()).containsExactly(before);
    }

    private static Trip trip(Country destination, Country... transits) {
        Trip trip = new Trip();
        trip.setCountry(destination);
        trip.setTransitCountries(List.of(transits));
        return trip;
    }

    private static Country country(long id, CountryFormalityTemplate... templates) {
        Country c = new Country();
        c.setId(id);
        for (CountryFormalityTemplate t : templates) {
            t.setCountry(c);
            c.getCountryFormalities().add(t);
        }
        return c;
    }

    private static CountryFormalityTemplate template(long id, long documentTypeId, boolean appliesInTransit) {
        CountryFormalityTemplate t = new CountryFormalityTemplate();
        t.setId(id);
        t.setDocumentType(documentType(documentTypeId));
        t.setType(FormalityType.FILE);
        t.setAppliesInTransit(appliesInTransit);
        return t;
    }

    private static DocumentType documentType(long id) {
        DocumentType dt = new DocumentType();
        dt.setId(id);
        return dt;
    }

    private static List<Long> documentTypeIds(Trip trip) {
        return trip.getFormalities().stream().map(f -> f.getDocumentType().getId()).toList();
    }

    private static List<Long> sourceTemplateIds(Trip trip) {
        return trip.getFormalities().stream().map(f -> f.getSourceTemplate().getId()).toList();
    }
}
