package fr.siovision.voyages.infrastructure.dto.rectification;

import java.time.Instant;
import java.util.UUID;

public record RectificationRequestDTO(
        UUID id,
        String field,
        String requestedValue,
        String reason,
        String status,
        Instant createdAt,
        Instant processedAt,
        String adminComment
) {
}
