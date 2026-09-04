package fr.siovision.voyages.infrastructure.dto;

public record TripRegistrationAdminViewDTO(
        Long registrationId,
        java.time.LocalDateTime registeredAt,
        String status,               // réutilise l’enum existant côté domaine (PENDING, ENROLLED, ...)
        RegistrationListUserDTO user,
        DocumentsSummaryDTO documentsSummary // peut être null si non demandé
) {}