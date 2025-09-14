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
    private Integer prixTotal; // en centimes d'euros
    private Integer participationDesFamilles; // en centimes d'euros
    private String coverPhotoUrl;
    private PaysDTO pays; // Détails du pays
    private DateRangeDTO datesVoyage; // Dates du voyage
    private Integer nombreMinParticipants;
    private Integer nombreMaxParticipants;
    private DateRangeDTO datesInscription;
    private List<FormaliteVoyageDTO> formalites;
    private List<OrganisateurDTO> organisateurs;
    private List<SectionDTO> sections;
    private String updatedAt; // Date de la dernière mise à jour
}
