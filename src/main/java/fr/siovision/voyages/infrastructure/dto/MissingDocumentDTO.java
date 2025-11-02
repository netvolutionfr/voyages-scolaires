package fr.siovision.voyages.infrastructure.dto;

public record MissingDocumentDTO(
    Integer totalRequired,
    Integer totalMissing,
    Iterable<TripMissingDocumentDTO> byTrip
) {
    /*
    {
    "totalRequired": 3,
    "totalMissing": 2,
    "byTrip": [{ "tripId": 253, "missing": 2 }]
  }
     */
}
