package fr.siovision.voyages.application.service.impl;

import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.client.AuthenticationExtensionsClientInputs;
import fr.siovision.voyages.application.service.ChallengeService;
import fr.siovision.voyages.config.AppProps;
import fr.siovision.voyages.domain.model.RegistrationAttempt;
import fr.siovision.voyages.infrastructure.dto.authentication.ChallengeResponse;
import fr.siovision.voyages.infrastructure.repository.RegistrationAttemptRepository;
import fr.siovision.voyages.application.utils.Base64Url;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.webauthn4j.data.*;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Service
public class ChallengeServiceImpl implements ChallengeService {
    private final RegistrationAttemptRepository attempts;
    private final SecureRandom rng = new SecureRandom();


    @Value("${app.challenge.ttl-seconds}")
    private long challengeTtlSeconds; // ex: 300 (5 minutes)

    @Value("${app.rp.id}")
    private String rpId; // ex: campusaway.fr (ou localhost en dev)

    @Value("${app.rp.name}")
    private String rpName; // ex: CampusAway

    @Value("${app.origin}")
    private String rpOrigin; // ex: https://app.campusaway.fr (ou http://localhost:5173 en dev)

    public ChallengeServiceImpl(RegistrationAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Override @Transactional
    public PublicKeyCredentialCreationOptions issue() {
        UUID uuid = UUID.randomUUID();
        byte[] chal = new byte[32]; rng.nextBytes(chal);
        var att = new RegistrationAttempt();
        att.setUuid(uuid);
        att.setChallenge(chal);
        att.setRpId(rpId);
        att.setOrigin(rpOrigin);
        att.setExpiresAt(Instant.now().plusSeconds(challengeTtlSeconds));
        att.setUsed(false);
        attempts.save(att);

        // Entités
        PublicKeyCredentialRpEntity rpEntity = new PublicKeyCredentialRpEntity(rpId, rpName);
        PublicKeyCredentialUserEntity userEntity = new PublicKeyCredentialUserEntity(
                uuid.toString().getBytes(),
                "user-"     + uuid,
                "Nouvel Utilisateur"
        );

        // 4. Paramètres de la Passkey (clé découvrable par défaut)
        AuthenticatorSelectionCriteria authenticatorSelection = new AuthenticatorSelectionCriteria(
                AuthenticatorAttachment.PLATFORM,
                true, // requireResidentKey = true pour une Passkey "découvrable",
                ResidentKeyRequirement.REQUIRED,
                UserVerificationRequirement.REQUIRED
        );

        List<PublicKeyCredentialParameters> pubKeyCredParams = List.of(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)
        );

        Challenge challenge = new DefaultChallenge(chal);

        return new PublicKeyCredentialCreationOptions(
                    rpEntity, // Relying Party
                    userEntity, // Utilisateur
                    challenge, // Challenge
                    pubKeyCredParams, // Types de clés publiques acceptées
                    challengeTtlSeconds * 1000, // Timeout en ms
                    List.of(), // ExcludeCredentials (vide ici, car pas de clés existantes à exclure)
                    authenticatorSelection, // Critères de sélection de l'authentificateur
                    List.of(), // Hints d'authentificateurs (vide ici)
                    AttestationConveyancePreference.NONE, // Pas besoin d'attestation
                    null // Pas d'extensions supplémentaires
                );
    }

    @Override @Transactional
    public byte[] consumeRegistrationChallengeFor(Long id, String email) {
        var att = attempts.findById(id).orElseThrow();
        if (att.isUsed() || att.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Challenge is either used or expired");
        }
        att.setEmailHint(email);
        att.setUsed(true);
        attempts.save(att);

        return att.getChallenge();
    }
}
