package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TripDetailDTO {
    private Long id;
    private String title;
    private String description;
    private String destination;
    private Integer familyContribution; // en centimes d'euros
    private String coverPhotoUrl;
    private CountryDTO country; // Détails du pays
    private DateRangeDTO tripDates;
    private Integer minParticipants;
    private Integer maxParticipants;
    private DateRangeDTO registrationDates;
    private Boolean poll; // true si le voyage est en mode "sondage" (dates non fixées)
    private List<TripFormalityDTO> formalities;
    private List<ChaperoneDTO> chaperones; // Détails des accompagnateurs
    private Long interestedCount; // Nombre de personnes intéressées
    private List<SectionDTO> sections;
    private Boolean interestedByCurrentUser; // Si l'utilisateur courant est intéressé
    private Boolean registeredByCurrentUser; // Si l'utilisateur courant est inscrit
    private String updatedAt; // Date de la dernière mise à jour
}
