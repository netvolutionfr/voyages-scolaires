package fr.siovision.voyages.infrastructure.dto;

public record HealthFormAdminDTO(
        boolean exists,
        String content // peut être null si exists=false
) {}