package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageDetailDTO {
    private Long id;
    private String nom;
    private String description;
    private String destination;
    private Integer participationDesFamilles; // en centimes d'euros
    private String coverPhotoUrl;
    private PaysDTO pays; // Détails du pays
    private DateRangeDTO datesVoyage; // Dates du voyage
    private Integer nombreMinParticipants;
    private Integer nombreMaxParticipants;
    private DateRangeDTO datesInscription;
    // Liste des formalités associées au voyage
    private List<FormaliteVoyageDTO> formalites;
}
