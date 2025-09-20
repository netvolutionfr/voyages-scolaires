package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenTripsResponse {
    private Long id;
    private String title;
    private String description;
    private String destination;
    private String departureDate;
    private String returnDate;
    private Integer minParticipants;
    private Integer maxParticipants;
    private String registrationDebutDate;
    private String registrationClosingDate;
}
