package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "section",
        indexes = {
                @Index(name = "ix_section_cycle", columnList = "cycle"),
                @Index(name = "ix_section_year", columnList = "year"),
                @Index(name = "ix_section_active", columnList = "is_active")
        },
        uniqueConstraints = {
                // Si "label" est unique globalement (tu importes une fois) :
                @UniqueConstraint(name = "ux_section_label", columnNames = {"label"}),
                // Si un jour tu ajoutes academic_year, remplace par (academic_year, label)
        }
)
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor @AllArgsConstructor
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId = UUID.randomUUID();

    private String label;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Cycle cycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "year", nullable = false, length = 16)
    private YearTag year;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Transient
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
