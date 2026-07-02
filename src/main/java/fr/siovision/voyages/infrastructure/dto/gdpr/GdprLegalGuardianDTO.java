package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.util.UUID;

public record GdprLegalGuardianDTO(
        UUID publicId,
        String fullName,
        String email
) {
}
