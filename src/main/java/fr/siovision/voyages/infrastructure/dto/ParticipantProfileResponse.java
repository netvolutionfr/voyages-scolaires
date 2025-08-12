package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.domain.model.Section;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantProfileResponse {
    private UUID id;
    private String nom;
    private String prenom;
    private String sexe; // M, F, Autre
    private String email;
    private String telephone;
    private String dateNaissance;
    private Long sectionId;
    private String sectionLibelle; // Nom de la section (optionnel, peut être ajouté si nécessaire)
    private UUID legalGuardianId; // ID du parent légal primaire (si mineur)
    private String legalGuardianName; // Nom du parent légal primaire (si mineur)
    private String legalGuardianEmail; // Email du parent légal primaire (si mineur)

    public ParticipantProfileResponse(Participant participant) {
        this.id = participant.getId();
        this.nom = participant.getNom();
        this.prenom = participant.getPrenom();
        this.sexe = participant.getSexe();
        this.email = participant.getEmail();
        this.telephone = participant.getTelephone();
        this.dateNaissance = participant.getDateNaissance() != null ? participant.getDateNaissance().toString() : null;

        Section section = participant.getSection();
        if (section != null) {
            this.sectionId = section.getId();
            this.sectionLibelle = section.getLibelle(); // Assurez-vous que la classe Section a une méthode getNom()
        } else {
            this.sectionId = null;
            this.sectionLibelle = null;
        }
        if (participant.getLegalGuardian() != null) {
            this.legalGuardianId = participant.getLegalGuardian().getId();
            this.legalGuardianName = participant.getLegalGuardian().getNom() + " " + participant.getLegalGuardian().getPrenom();
            this.legalGuardianEmail = participant.getLegalGuardian().getEmail();
        } else {
            this.legalGuardianId = null;
            this.legalGuardianName = null;
            this.legalGuardianEmail = null;
        }
    }
}
