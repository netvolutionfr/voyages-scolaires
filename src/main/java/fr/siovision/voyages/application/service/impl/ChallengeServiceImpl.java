package fr.siovision.voyages.application.service.impl;

import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import fr.siovision.voyages.application.service.ChallengeService;
import fr.siovision.voyages.domain.model.RegistrationAttempt;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.RegistrationAttemptRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.webauthn4j.data.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class ChallengeServiceImpl implements ChallengeService {
    private final RegistrationAttemptRepository attempts;
    private final SecureRandom rng = new SecureRandom();
    private final UserRepository userRepository;


    @Value("${webauthn.challenge.ttl-seconds}")
    private long challengeTtlSeconds; // ex: 300 (5 minutes)

    @Value("${webauthn.rp.id}")
    private String rpId; // ex: campusaway.fr (ou localhost en dev)

    @Value("${webauthn.rp.name}")
    private String rpName; // ex: CampusAway

    @Value("${webauthn.allowed-origins}")
    private List<String> rpOrigins; // ex: https://app.campusaway.fr (ou http://localhost:5173 en dev)

    public ChallengeServiceImpl(
            RegistrationAttemptRepository attempts,
            UserRepository userRepository
    ) {
        this.attempts = attempts;
        this.userRepository = userRepository;
    }

    @Override @Transactional
    public PublicKeyCredentialCreationOptions issue(String email, String rpOrigin) {

        if (!rpOrigins.contains(rpOrigin)) {
            throw new IllegalArgumentException("Origin non autorisée: " + rpOrigin);
        }

        UUID uuid = UUID.randomUUID();
        byte[] chal = new byte[32]; rng.nextBytes(chal);
        var att = new RegistrationAttempt();
        att.setUuid(uuid);
        att.setChallenge(chal);
        att.setEmailHint(email);
        att.setRpId(rpId);
        att.setOrigin(rpOrigin);
        att.setExpiresAt(Instant.now().plusSeconds(challengeTtlSeconds));
        att.setUsed(false);
        attempts.save(att);

        User user = userRepository.findByEmail(email).orElseThrow(); // Renvoyer un 404 si l'email n'existe pas

        // Entités
        PublicKeyCredentialRpEntity rpEntity = new PublicKeyCredentialRpEntity(rpId, rpName);
        PublicKeyCredentialUserEntity userEntity = new PublicKeyCredentialUserEntity(
                uuid.toString().getBytes(),
                email,
                user.getFirstName() + " " + user.getLastName()
        );

        // 4. Paramètres de la Passkey (clé découvrable par défaut)

        return getPublicKeyCredentialCreationOptions(chal, rpEntity, userEntity);
    }

    @Override @Transactional
    public PublicKeyCredentialCreationOptions issueIOS(String rpOrigin) {

        if (!rpOrigins.contains(rpOrigin)) {
            throw new IllegalArgumentException("Origin non autorisée: " + rpOrigin);
        }

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
                uuid.toString(),
                ""
        );

        // 4. Paramètres de la Passkey (clé découvrable par défaut)

        return getPublicKeyCredentialCreationOptions(chal, rpEntity, userEntity);
    }

    private PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions(byte[] chal, PublicKeyCredentialRpEntity rpEntity, PublicKeyCredentialUserEntity userEntity) {
        AuthenticatorSelectionCriteria authenticatorSelection = this.getAuthenticatorSelectionCriteria();
        List<PublicKeyCredentialParameters> pubKeyCredParams = this.getPubKeyCredParams();
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


    public AuthenticatorSelectionCriteria getAuthenticatorSelectionCriteria() {
        return new AuthenticatorSelectionCriteria(
                AuthenticatorAttachment.PLATFORM,
                true, // requireResidentKey = true pour une Passkey "découvrable",
                ResidentKeyRequirement.REQUIRED,
                UserVerificationRequirement.REQUIRED
        );
    }

    @Override
    public void invalidateChallenge(RegistrationAttempt attempt) {
        attempts.delete(attempt);
    }

    public List<PublicKeyCredentialParameters> getPubKeyCredParams() {
        return List.of(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)
        );
    }

    @Override
    public Optional<RegistrationAttempt> getChallenge(Challenge myChallenge) {
        if (myChallenge == null || myChallenge.getValue().length == 0) {
            return Optional.empty();
        }
        RegistrationAttempt attempt = attempts.findByChallenge(myChallenge.getValue())
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
        return Optional.ofNullable(attempt);
    }

    /**
     * Tâche planifiée pour purger les challenges expirés.
     * La tâche est exécutée toutes les 10 minutes (600 000 ms).
     */
    @Transactional // Nécessaire si vous effectuez des opérations de BDD
    @Scheduled(fixedRate = 600000) // 600000 millisecondes = 10 minutes
    // OU @Scheduled(cron = "0 0/10 * * * ?") pour une expression Cron
    public void cleanupExpiredChallenges() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(15);

        // C'est ici que vous appelez votre repository
        int deletedCount = attempts.deleteByCreatedAtBefore(expirationThreshold);

        System.out.println("Purge de challenges terminée. " + deletedCount + " enregistrements supprimés.");
    }
}
