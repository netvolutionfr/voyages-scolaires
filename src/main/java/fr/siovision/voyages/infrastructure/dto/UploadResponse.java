package fr.siovision.voyages.infrastructure.dto;

public record UploadResponse(
        String documentId,
        String status
) {}