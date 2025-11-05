package fr.siovision.voyages.infrastructure.dto;

import java.util.UUID;

public record UserMiniDTO(
        UUID publicId,
        String firstName,
        String lastName,
        String email,
        String telephone,
        SectionMiniDTO section
) {}