package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DateRangeDTO {
    private LocalDate from; // Date de début au format ISO 8601 (ex: "2025-08-28T22:00:00.000Z")
    private LocalDate to;   // Date de fin au format ISO 8601 (ex: "2025-08-29T22:00:00.000Z")
}
