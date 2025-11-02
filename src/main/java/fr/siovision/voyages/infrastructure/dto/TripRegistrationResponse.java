package fr.siovision.voyages.infrastructure.dto;

public record TripRegistrationResponse(
        Long id,            // TripUser id
        Long tripId,
        String status       // ENROLLED / CONFIRMED / ...
) {}