package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.TripRegistrationStatus;

public record RegistrationAdminUpdateRequest(
        TripRegistrationStatus status,
        String adminMessage
) {}