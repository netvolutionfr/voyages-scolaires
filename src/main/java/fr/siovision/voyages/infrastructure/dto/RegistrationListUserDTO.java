package fr.siovision.voyages.infrastructure.dto;

import java.util.UUID;

public record RegistrationListUserDTO(
        UUID publicId,
        String firstName,
        String lastName,
        SectionMiniDTO section
) {}
