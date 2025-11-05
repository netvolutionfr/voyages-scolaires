package fr.siovision.voyages.infrastructure.dto;

public record RegistrationAdminDetailDTO(
        Long registrationId,
        TripMiniDTO trip,
        UserMiniDTO user,
        String status,
        java.time.LocalDateTime registeredAt
) {}