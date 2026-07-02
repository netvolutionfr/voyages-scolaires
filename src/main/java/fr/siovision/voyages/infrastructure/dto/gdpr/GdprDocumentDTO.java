package fr.siovision.voyages.infrastructure.dto.gdpr;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GdprDocumentDTO(
        String type,
        String originalFilename,
        String mime,
        Long size,
        String sha256,
        String fileNumber,
        LocalDate deliveryDate,
        LocalDate expirationDate,
        LocalDateTime createdAt
) {
}
