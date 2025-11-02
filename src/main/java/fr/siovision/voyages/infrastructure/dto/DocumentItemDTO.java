package fr.siovision.voyages.infrastructure.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record DocumentItemDTO(
        DocumentTypeDetailDTO documentType,
        boolean required,
        List<RequiredByTripsDTO> requiredByTrips,
        boolean provided,
        Optional<Instant> providedAt,
        Optional<DocumentObjectDTO> lastObject,
        List<DocumentWarningDTO> warnings
) {
    /*


    {
      "documentType": {
        "id": 10, "code": "passport", "label": "Passeport",
        "kind": "FILE",
        "acceptedMime": ["application/pdf","image/jpeg","image/png"],
        "maxSizeMb": 10,
        "scope": "GENERAL"
      },
      "required": true,
      "requiredByTrips": [
        { "tripId": 253, "label": "San Francisco (8–15 avr. 2026)", "deadline": "2026-02-07" }
      ],
      "provided": true,
      "providedAt": "2025-11-10T19:21:03Z",
      "lastObject": { "id": "b2f7…", "mime": "application/pdf", "status": "READY", "size": 342100 },
      "warnings": [{ "code": "EXPIRES_SOON", "message": "Expire < 6 mois après le retour" }]
    }
     */
}
