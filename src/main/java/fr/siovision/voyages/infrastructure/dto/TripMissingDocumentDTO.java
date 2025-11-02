package fr.siovision.voyages.infrastructure.dto;

public record TripMissingDocumentDTO(
    Integer tripId,
    Integer missing
) {
    /*
    { "tripId": 253, "missing": 2 }
     */
}
