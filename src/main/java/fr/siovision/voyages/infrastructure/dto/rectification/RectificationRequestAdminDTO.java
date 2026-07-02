package fr.siovision.voyages.infrastructure.dto.rectification;

import java.time.Instant;
import java.util.UUID;

public record RectificationRequestAdminDTO(
        UUID id,
        UUID userPublicId,
        String userFullName,
        String userEmail,
        String field,
        String requestedValue,
        String reason,
        String status,
        Instant createdAt
) {
}
