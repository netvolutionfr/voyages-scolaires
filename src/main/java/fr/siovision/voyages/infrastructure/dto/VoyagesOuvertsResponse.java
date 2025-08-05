package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyagesOuvertsResponse {
    private Long id;
    private String nom;
    private String description;
    private String destination;
    private String dateDepart;
    private String dateRetour;
    private Integer nombreMinParticipants;
    private Integer nombreMaxParticipants;
    private String dateDebutInscription;
    private String dateFinInscription;
}
