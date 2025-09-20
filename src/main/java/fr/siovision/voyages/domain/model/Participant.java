package fr.siovision.voyages.domain.model;

import fr.siovision.voyages.infrastructure.converter.CryptoConverter;
import fr.siovision.voyages.infrastructure.converter.CryptoDateConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "participant",
        uniqueConstraints = @UniqueConstraint(name="uk_participant_student_user", columnNames = "student_user_id"))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable=false)
    private String lastName;

    @Column(nullable=false)
    private String firstName;

    @Convert(converter = CryptoConverter.class)
    @Column(nullable=false)
    private String gender; // "M", "F", "N"

    @Column(nullable=false)
    private String email;

    @Convert(converter = CryptoConverter.class)
    private String telephone;

    @Convert(converter = CryptoDateConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDate birthDate;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="student_user_id", unique = true, nullable = false)
    private User studentAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="legal_guardian_user_id") // parent ou tuteur légal, null si majeur
    private User legalGuardian;

    // pratique : cache de l’email de contact principal (peut rester nul)
    private String primaryContactEmail;

    @OneToMany
    private List<Document> documents;

    @OneToMany(mappedBy = "participant")
    private List<TripParticipant> trips;

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
