package fr.siovision.voyages.infrastructure.dto;

import java.util.List;

public record DocumentsDTO(
    Long userId,
    List<TripSummaryDTO> trip,
    List<DocumentItemDTO> items,
    MissingDocumentDTO missing
) {
        /*
{
  "userId": 1234,
  "trips": [
    { "id": 253, "label": "San Francisco & Silicon Valley", "from": "2026-04-08", "to": "2026-04-15", "countryId": 14 }
  ],
  "items": [
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
    },
    {
      "documentType": {
        "id": 11, "code": "us_esta", "label": "Autorisation ESTA",
        "kind": "FILE",
        "acceptedMime": ["application/pdf","image/jpeg","image/png"],
        "maxSizeMb": 5,
        "scope": "TRIP"
      },
      "required": true,
      "requiredByTrips": [
        { "tripId": 253, "label": "San Francisco (8–15 avr. 2026)", "deadline": "2026-03-09" }
      ],
      "provided": false
    },
    {
      "documentType": { "id": 7, "code": "health_form", "label": "Fiche sanitaire", "kind": "FORM", "scope": "TRIP" },
      "required": true,
      "requiredByTrips": [{ "tripId": 253, "label": "San Francisco (8–15 avr. 2026)", "deadline": "2026-03-19" }],
      "provided": false
    }
  ],
  "missing": {
    "totalRequired": 3,
    "totalMissing": 2,
    "byTrip": [{ "tripId": 253, "missing": 2 }]
  }
}
     */

}
