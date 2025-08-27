package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageDTO {
    private Long id;
    private String nom;
    private String description;
    private String destination;
    private Long paysId; // Référence vers l'entité Pays
    private DateRangeDTO datesVoyage; // Dates du voyage
    private int nombreMinParticipants;
    private int nombreMaxParticipants;
    private DateRangeDTO datesInscription; // Dates d'inscription
}
