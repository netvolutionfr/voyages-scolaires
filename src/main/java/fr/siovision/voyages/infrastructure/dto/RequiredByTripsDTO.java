package fr.siovision.voyages.infrastructure.dto;

public record RequiredByTripsDTO(
    Long tripId,
    String label
) {
    /*
    { "tripId": 253, "label": "San Francisco (8–15 avr. 2026)" }
     */
}
