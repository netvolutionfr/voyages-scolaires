package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.util.List;

public record GdprTripsDTO(
        List<GdprTripRegistrationDTO> registrations,
        List<GdprTripPreferenceDTO> preferences
) {
}
