package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DateRangeDTO {
    private String from; // Date de début au format ISO 8601 (ex: "2025-08-28T22:00:00.000Z")
    private String to;   // Date de fin au format ISO 8601 (ex: "2025-08-29T22:00:00.000Z")
}
