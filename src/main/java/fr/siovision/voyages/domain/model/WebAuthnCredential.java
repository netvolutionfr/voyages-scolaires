package fr.siovision.voyages.domain.model;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import jakarta.persistence.*;
import lombok.*;

import java.nio.charset.StandardCharsets;
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

    @Column(name="cose_key", columnDefinition="bytea", nullable = false)
    private byte[] coseKey;

    private long signatureCount;

    @Column(columnDefinition = "bytea")
    private byte[] userHandle;

    // --- Champs pour la traçabilité ---
    private String aaguid;

    // --- Constructeur pour la persistance ---
    public WebAuthnCredential(User user, RegistrationData registrationData, ObjectConverter objectConverter, String userId) {
        this.user = Objects.requireNonNull(user, "user");
        Objects.requireNonNull(registrationData.getAttestationObject(), "attestationObject");

        var attObj = registrationData.getAttestationObject();
        var authData = attObj.getAuthenticatorData();
        AttestedCredentialData acd = Objects.requireNonNull(
                authData.getAttestedCredentialData(), "attestedCredentialData");

        // ID du credential
        this.credentialId = Objects.requireNonNull(acd.getCredentialId(), "credentialId");

        // COSEKey -> CBOR (à stocker en base)
        this.coseKey = objectConverter.getCborConverter().writeValueAsBytes(acd.getCOSEKey());

        // Compteur initial
        this.signatureCount = authData.getSignCount();

        // userHandle
        this.userHandle = userId.getBytes(StandardCharsets.UTF_8);

        // AAGUID
        this.aaguid = acd.getAaguid().toString();
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