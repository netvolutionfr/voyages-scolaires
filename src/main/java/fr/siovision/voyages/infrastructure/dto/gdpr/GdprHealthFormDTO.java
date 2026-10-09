package fr.siovision.voyages.infrastructure.dto.gdpr;


import java.time.Instant;

public record GdprHealthFormDTO(
        Instant signedAt,
        Instant validUntil,
        Object payload
) {
}
