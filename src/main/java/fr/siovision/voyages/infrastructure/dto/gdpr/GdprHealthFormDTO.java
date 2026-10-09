package fr.siovision.voyages.infrastructure.dto.gdpr;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record GdprHealthFormDTO(
        Instant signedAt,
        Instant validUntil,
        JsonNode payload
) {
}
