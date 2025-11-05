package fr.siovision.voyages.infrastructure.dto;

public record DocumentObjectMiniDTO(
        String id,          // UUID/ULID string
        long size,
        String mime,
        boolean previewable
) {}