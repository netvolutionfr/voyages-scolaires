package fr.siovision.voyages.infrastructure.dto;

import java.util.List;

public record DocumentsAdminDTO(
        Long userId,
        Long tripId,
        DocumentsSummaryDTO summary,
        List<DocumentItemAdminDTO> items
) {}