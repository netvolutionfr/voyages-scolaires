package fr.siovision.voyages.infrastructure.dto;

public record RegistrationAdminUpdateResponse(
        Long id,
        Long tripId,
        Long userId,
        String status
) {}