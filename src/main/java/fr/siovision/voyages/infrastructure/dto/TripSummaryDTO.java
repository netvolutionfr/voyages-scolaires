package fr.siovision.voyages.infrastructure.dto;

public record TripSummaryDTO(
    Long id,
    String title,
    Long countryId
) {
}
