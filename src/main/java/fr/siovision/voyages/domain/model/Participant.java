package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    @Id
    @GeneratedValue
    private Long id;
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
    private Boolean accompagnateur;

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

    @OneToMany
    private List<Document> documents;

    @OneToMany(mappedBy = "participant")
    private List<VoyageParticipant> voyages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
