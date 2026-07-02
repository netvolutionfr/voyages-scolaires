package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.time.LocalDate;

public record GdprConsentDTO(
        LocalDate givenAt,
        String text
) {
}
