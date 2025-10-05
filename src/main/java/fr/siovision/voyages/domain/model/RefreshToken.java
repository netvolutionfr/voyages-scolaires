package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_user", columnList = "user_id"),
                @Index(name = "idx_refresh_status", columnList = "status")
        })
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hash du token (SHA-256), stocké en binaire */
    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "bytea")
    private byte[] tokenHash;

    /** Identifiant de famille pour révoquer tous les refresh liés (logout-all) */
    @Builder.Default
    @Column(name = "family_id", nullable = false, columnDefinition = "uuid")
    private UUID familyId = UUID.randomUUID();

    /** Date/heure d’émission */
    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    /** Date/heure d’expiration */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Date/heure de dernière utilisation (refresh) */
    @UpdateTimestamp
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** État : ACTIVE, ROTATED, REVOKED */
    @Column(length = 16, nullable = false)
    @ColumnDefault("'ACTIVE'")
    private String status = "ACTIVE";

    /** Lien vers le nouveau token émis lors d’une rotation (optionnel) */
    @OneToOne
    @JoinColumn(name = "replaced_by_id")
    private RefreshToken replacedBy;

    @PrePersist
    void ensureFamilyId() {
        if (familyId == null) familyId = UUID.randomUUID();
    }

    /* --- Constructeur pratique --- */
    public RefreshToken(User user, byte[] hash, Instant expiresAt, String status) {
        this.user = user;
        this.tokenHash = hash;
        this.expiresAt = expiresAt;
        this.status = status;
        this.familyId = UUID.randomUUID();
        this.issuedAt = Instant.now();
    }
}