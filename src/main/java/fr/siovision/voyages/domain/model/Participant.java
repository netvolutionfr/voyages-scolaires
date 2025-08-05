package fr.siovision.voyages.domain.model;

import fr.siovision.voyages.infrastructure.converter.CryptoConverter;
import fr.siovision.voyages.infrastructure.converter.CryptoDateConverter;
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
    @Convert(converter = CryptoConverter.class)
    private String sexe; // M, F, Autre
    private String email;
    @Convert(converter = CryptoConverter.class)
    private String telephone;
    @Convert(converter = CryptoConverter.class)
    private String adresse;
    @Convert(converter = CryptoConverter.class)
    private String codePostal;
    @Convert(converter = CryptoConverter.class)
    private String ville;
    @Convert(converter = CryptoDateConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDate dateNaissance;
    private String section;

    // parent 1
    @Convert(converter = CryptoConverter.class)
    @Convert(converter = CryptoConverter.class)
    private String parent1Nom;
    @Convert(converter = CryptoConverter.class)
    private String parent1Prenom;
    @Convert(converter = CryptoConverter.class)
    private String parent1Email;
    @Convert(converter = CryptoConverter.class)
    private String parent1Telephone;

    // parent 2
    @Convert(converter = CryptoConverter.class)
    private String parent2Nom;
    @Convert(converter = CryptoConverter.class)
    private String parent2Prenom;
    @Convert(converter = CryptoConverter.class)
    private String parent2Email;
    @Convert(converter = CryptoConverter.class)
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
