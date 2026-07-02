package fr.siovision.voyages.infrastructure.dto.gdpr;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record GdprHealthFormDTO(
        Instant signedAt,
        Instant validUntil,
        JsonNode payload
) {
}
