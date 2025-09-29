package fr.siovision.voyages.domain.model;

import com.webauthn4j.credential.CredentialRecord;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

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

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private CredentialRecord credentialRecord;

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