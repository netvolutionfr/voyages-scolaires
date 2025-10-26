package fr.siovision.voyages.domain.model;

import fr.siovision.voyages.infrastructure.converter.CryptoConverter;
import fr.siovision.voyages.infrastructure.converter.CryptoDateConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId = UUID.randomUUID();

    @Column(unique = true)
    String email; // email de l'utilisateur, utilisé pour l'authentification
    String lastName; // nom de famille de l'utilisateur
    String firstName; // prénom de l'utilisateur
    String displayName; // nom complet affiché (prénom + nom) utilisé par iOS lors de la création de passkey

    @Convert(converter = CryptoConverter.class)
    private String gender; // "M", "F", "N"

    @Convert(converter = CryptoConverter.class)
    private String telephone;

    @Convert(converter = CryptoDateConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    UserRole role; // PARENT, STUDENT, ADMIN

    @Enumerated(EnumType.STRING)
    UserStatus status; // ACTIVE, INACTIVE, PENDING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="legal_guardian_user_id") // parent ou tuteur légal, null si majeur
    private User legalGuardian;

    @OneToMany
    private List<Document> documents;

    @OneToMany(mappedBy = "user")
    private List<TripUser> trips;

    LocalDate consentGivenAt;
    String consentText;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TripPreference> tripPreferences = new HashSet<>();

    // Section
    @ManyToOne
    @JoinColumn(name = "section_id") // Foreign key vers la table Section, nullable si l'utilisateur n'est pas un élève
    private Section section;

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

    public void markAsVerified() {
        this.status = UserStatus.ACTIVE;
    }
}