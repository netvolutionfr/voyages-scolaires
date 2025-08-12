package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @GeneratedValue
    @Id
    private UUID id;

    @Column(unique = true)
    String keycloakId; // `sub` du token JWT

    @Column(unique = true)
    String email; // email de l'utilisateur, utilisé pour l'authentification
    String nom; // nom de famille de l'utilisateur
    String prenom; // prénom de l'utilisateur
    String telephone;

    // Enfants
    @ManyToOne
    @JoinColumn(name = "parent_user_id")
    Participant participant; // Lien vers le participant associé

    @Enumerated(EnumType.STRING)
    UserRole role; // PARENT, STUDENT, ADMIN

    LocalDate consentGivenAt;
    String consentText;

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