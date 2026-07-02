package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.time.LocalDateTime;
import java.util.UUID;

public record GdprProfileDTO(
        UUID publicId,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String gender,
        String birthDate,
        String telephone,
        String role,
        String status,
        String section,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
