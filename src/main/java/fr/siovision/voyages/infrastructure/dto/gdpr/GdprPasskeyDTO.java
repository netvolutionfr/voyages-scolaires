package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.time.LocalDateTime;

public record GdprPasskeyDTO(
        LocalDateTime createdAt,
        String aaguid
) {
}
