package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.util.UUID;

public record GdprFamilyLinkDTO(
        String relation,
        UUID publicId,
        String fullName,
        String email
) {
}
