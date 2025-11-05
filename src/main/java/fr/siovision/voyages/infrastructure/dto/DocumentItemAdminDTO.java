package fr.siovision.voyages.infrastructure.dto;

import java.time.LocalDateTime;

public record DocumentItemAdminDTO(
        DocumentTypeAdminDTO documentType,
        boolean required,
        boolean provided,
        LocalDateTime providedAt,
        DocumentObjectMiniDTO lastObject
) {}