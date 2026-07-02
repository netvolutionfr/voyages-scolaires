package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GdprTripRegistrationDTO(
        String tripTitle,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        String registrationStatus,
        LocalDateTime registrationDate,
        LocalDateTime decisionDate,
        String decisionMessage
) {
}
