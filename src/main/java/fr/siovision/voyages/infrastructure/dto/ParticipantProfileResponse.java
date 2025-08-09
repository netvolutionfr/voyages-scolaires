package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantProfileResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String sexe; // M, F, Autre
    private String email;
    private String telephone;
    private String dateNaissance;
    private String section;

    // parent 1
    private String parent1Nom;
    private String parent1Prenom;
    private String parent1Email;
    private String parent1Telephone;

    // parent 2
    private String parent2Nom;
    private String parent2Prenom;
    private String parent2Email;
    private String parent2Telephone;
}
