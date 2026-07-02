package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.time.Instant;
import java.util.List;

public record GdprSecurityDTO(
        List<GdprPasskeyDTO> passkeys,
        Instant lastLoginAt
) {
}
