package fr.siovision.voyages.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportRow {
    private String role;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String sexe;
    private String section;
    private String dateNaissance;
    private String parent1Nom;
    private String parent1Prenom;
    private String parent1Email;
    private String parent1Tel;
    private String parent2Nom;
    private String parent2Prenom;
    private String parent2Email;
    private String parent2Tel;
}