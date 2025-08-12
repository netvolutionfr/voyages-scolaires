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
import java.util.UUID;

@Entity
@Table(name = "participant",
        uniqueConstraints = @UniqueConstraint(name="uk_participant_student_user", columnNames = "student_user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable=false)
    private String nom;

    @Column(nullable=false)
    private String prenom;

    @Convert(converter = CryptoConverter.class)
    @Column(nullable=false)
    private String sexe; // M, F, Autre

    @Column(nullable=false)
    private String email;

    @Convert(converter = CryptoConverter.class)
    private String telephone;

    @Convert(converter = CryptoDateConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDate dateNaissance;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // parent légal primaire (si mineur)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_user_id")
    private User legalGuardian;

    // compte de l'élève (optionnel : mineur avec accès; obligatoire si majeur autonome)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="student_user_id", unique = true)
    private User studentAccount;

    // pratique : cache de l’email de contact principal (peut rester nul)
    private String primaryContactEmail;

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
