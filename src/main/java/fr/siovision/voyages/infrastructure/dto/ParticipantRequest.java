package fr.siovision.voyages.infrastructure.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ParticipantRequest {
    private String nom;
    private String prenom;
    private String sexe; // M, F, Autre
    private String email;
    private String telephone;
    private String adresse;
    private String codePostal;
    private String ville;
    private LocalDate dateNaissance;
    private String section;

    private String parent1Nom;
    private String parent1Prenom;
    private String parent1Email;
    private String parent1Telephone;

    private String parent2Nom;
    private String parent2Prenom;
    private String parent2Email;
    private String parent2Telephone;
}
