package fr.siovision.voyages.infrastructure.dto.gdpr;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

public record GdprHealthFormDTO(
        Instant signedAt,
        Instant validUntil,
        @Schema(implementation = Map.class) JsonNode payload
) {
}
