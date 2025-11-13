package fr.siovision.voyages.infrastructure.dto;

public record DocumentItemAdminDTO(
        DocumentTypeAdminDTO documentType,
        boolean required,
        boolean provided,
        java.util.Date providedAt,
        DocumentObjectMiniDTO lastObject
) {}