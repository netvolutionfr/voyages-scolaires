package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VoyageRequest {
    private String nom;
    private String description;
    private String destination;
    private LocalDate dateDepart;
    private LocalDate dateRetour;
    private Integer nombreMinParticipants;
    private Integer nombreMaxParticipants;
    private LocalDate dateDebutInscription;
    private LocalDate dateFinInscription;
}
