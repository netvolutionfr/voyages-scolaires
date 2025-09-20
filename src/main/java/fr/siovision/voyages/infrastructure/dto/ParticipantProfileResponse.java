package fr.siovision.voyages.infrastructure.dto;

import fr.siovision.voyages.domain.model.Participant;
import fr.siovision.voyages.domain.model.Section;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantProfileResponse {
    private Long id;
    private String lastName;
    private String firstName;
    private String gender; // M, F, N
    private String email;
    private String telephone;
    private String birthDate;
    private Long sectionId;
    private String sectionLabel; // Nom de la section (optionnel, peut être ajouté si nécessaire)
    private Long legalGuardianId; // ID du parent légal primaire (si mineur)
    private String legalGuardianName; // Nom du parent légal primaire (si mineur)
    private String legalGuardianEmail; // Email du parent légal primaire (si mineur)

    public ParticipantProfileResponse(Participant participant) {
        this.id = participant.getId();
        this.lastName = participant.getLastName();
        this.firstName = participant.getFirstName();
        this.gender = participant.getGender();
        this.email = participant.getEmail();
        this.telephone = participant.getTelephone();
        this.birthDate = participant.getBirthDate() != null ? participant.getBirthDate().toString() : null;

        Section section = participant.getSection();
        if (section != null) {
            this.sectionId = section.getId();
            this.sectionLabel = section.getLabel(); // Assurez-vous que la classe Section a une méthode getNom()
        } else {
            this.sectionId = null;
            this.sectionLabel = null;
        }
        if (participant.getLegalGuardian() != null) {
            this.legalGuardianId = participant.getLegalGuardian().getId();
            this.legalGuardianName = participant.getLegalGuardian().getLastName() + " " + participant.getLegalGuardian().getFirstName();
            this.legalGuardianEmail = participant.getLegalGuardian().getEmail();
        } else {
            this.legalGuardianId = null;
            this.legalGuardianName = null;
            this.legalGuardianEmail = null;
        }
    }
}
