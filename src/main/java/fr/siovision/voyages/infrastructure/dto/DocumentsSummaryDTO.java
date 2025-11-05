package fr.siovision.voyages.infrastructure.dto;

public record DocumentsSummaryDTO(
        int required,
        int provided,
        int missing
) {
    public static DocumentsSummaryDTO empty() { return new DocumentsSummaryDTO(0,0,0); }
}