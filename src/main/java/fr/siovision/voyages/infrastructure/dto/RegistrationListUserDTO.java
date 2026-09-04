package fr.siovision.voyages.infrastructure.dto;

import java.util.UUID;

/** Vue liste : pas de coordonnées (email/téléphone), réservées à l'endpoint de détail. */
public record RegistrationListUserDTO(
        UUID publicId,
        String firstName,
        String lastName,
        SectionMiniDTO section
) {}
