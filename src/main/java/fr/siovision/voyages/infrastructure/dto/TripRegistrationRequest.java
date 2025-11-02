package fr.siovision.voyages.infrastructure.dto;

public record TripRegistrationRequest(
        Long tripId,
        boolean agreeToCommitments
) {}