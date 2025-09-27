package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // nullable pendant l’inscription si tu veux stocker “orphelin”, sinon lie seulement si autorisé

    @Column(unique = true, nullable = false)
    private String credentialId; // base64url, unique

    @Column(nullable = false)
    private byte[] publicKeyCose;

    @Column(nullable = false)
    private long signCount;
    private String aaguid; // UUID as String
    private String transports; // csv

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