package fr.siovision.voyages.domain.model;

import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

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
    private User user;

    @Column(columnDefinition = "bytea")
    private byte[] credentialId;

    @Column(columnDefinition = "bytea")
    private byte[] publicKey;

    private long signatureCount;

    @Column(columnDefinition = "bytea")
    private byte[] userHandle;

    // --- Champs pour la traçabilité ---
    private String aaguid;

    // --- Constructeur pour la persistance ---
    public WebAuthnCredential(User user, RegistrationData registrationData) {
        this.user = user;

        assert registrationData.getAttestationObject() != null;
        AttestedCredentialData attestedCredentialData = registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
        assert attestedCredentialData != null;
        this.credentialId = attestedCredentialData.getCredentialId();
        this.publicKey = Objects.requireNonNull(attestedCredentialData.getCOSEKey().getPublicKey()).getEncoded();
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData = registrationData.getAttestationObject().getAuthenticatorData();
        this.signatureCount = authenticatorData.getSignCount();
        this.userHandle = user.getPublicId().toString().getBytes(); // Assumer que user.getPublicId() est une chaîne unique pour l'utilisateur
        this.aaguid = attestedCredentialData.getAaguid().toString();
    }
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