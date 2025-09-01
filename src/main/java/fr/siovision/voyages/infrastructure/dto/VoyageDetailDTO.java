package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.FormaliteVoyage;
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
    private PaysDTO pays; // Détails du pays
    private DateRangeDTO datesVoyage; // Dates du voyage
    private int nombreMinParticipants;
    private int nombreMaxParticipants;
    private DateRangeDTO datesInscription;
    // Liste des formalités associées au voyage
    private List<FormaliteVoyageDTO> formalites;
}
