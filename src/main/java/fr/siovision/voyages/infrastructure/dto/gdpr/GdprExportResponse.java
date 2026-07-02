package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.time.Instant;
import java.util.List;

public record GdprExportResponse(
        Instant exportedAt,
        String format,
        GdprProfileDTO profile,
        GdprConsentDTO consent,
        GdprLegalGuardianDTO legalGuardian,
        List<GdprFamilyLinkDTO> familyLinks,
        GdprTripsDTO trips,
        List<GdprDocumentDTO> documents,
        GdprHealthFormDTO healthForm,
        GdprSecurityDTO security
) {
}
