package fr.siovision.voyages.application.service.impl;

import com.webauthn4j.data.*;
import com.webauthn4j.verifier.exception.VerificationException;
import fr.siovision.voyages.application.service.ChallengeService;
import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.application.service.RegistrationFlowService;
import fr.siovision.voyages.domain.model.RegistrationAttempt;
import fr.siovision.voyages.domain.model.UserStatus;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishResponse;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import fr.siovision.voyages.infrastructure.repository.WebAuthnCredentialRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.webauthn4j.WebAuthnRegistrationManager;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.data.client.challenge.Challenge;

import java.util.*;
import java.util.stream.Collectors;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.WebAuthnCredential;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistrationFlowServiceImpl implements RegistrationFlowService {

    private final UserRepository userRepository;
    private final WebAuthnCredentialRepository credentialRepository;
    private final ChallengeService regChallengeService;
    private final JwtService jwtService;

    // WebAuthn4J registration manager (non-strict = accepte plus d’attestations)
    private final WebAuthnRegistrationManager webAuthnManager =
            WebAuthnRegistrationManager.createNonStrictWebAuthnRegistrationManager();
    private final ChallengeService challengeService;

    @Value("${webauthn.allowed-origins}")          // ex: https://app.campusaway.fr (ou http://localhost:5173 en dev)
    private List<String> allowedOrigins;

    @Value("${webauthn.rp.id}")       // ex: campusaway.fr (ou localhost en dev)
    private String rpId;

    @Override
    public RegisterFinishResponse finishRegistration(String registrationRequest, String appOrigin) {

        // 0) Parser et valider la requête
        RegistrationData registrationData;
        try {
            registrationData = webAuthnManager.parse(registrationRequest);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid registration request: " + e.getMessage(), e);
        }

        if (!allowedOrigins.contains(appOrigin)) {
            throw new IllegalArgumentException("Origin non autorisée: " + appOrigin);
        }

        // 1) Récupérer le contexte serveur (challenge attendu + rpId + origin)
        Origin origin = new Origin(appOrigin);
        String rpId = this.rpId;

        // 2) Récupérer le challenge dans registration_attempt
        assert registrationData.getCollectedClientData() != null;
        Challenge expectedChallenge = registrationData.getCollectedClientData().getChallenge();
        RegistrationAttempt attempt = regChallengeService.getChallenge(expectedChallenge)
                .orElseThrow(() -> new IllegalStateException("Challenge de registration introuvable"));

        User user = userRepository.findByEmail(attempt.getEmailHint())
                .orElseThrow(() -> new IllegalStateException("User not found for email: " + attempt.getEmailHint()));

        ServerProperty serverProperty = new ServerProperty(origin, rpId, expectedChallenge);

        // expectations
        List<PublicKeyCredentialParameters> pubKeyCredParams = challengeService.getPubKeyCredParams();

        boolean userVerificationRequired = true; // toujours true Sécurité maximale. L'authentificateur doit vérifier l'utilisateur (biométrie/PIN)
        boolean userPresenceRequired = true; // demande à l'authentificateur de prouver que l'utilisateur est physiquement présent et interagit avec le dispositif au moment de l'opération

        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty, pubKeyCredParams, userVerificationRequired, userPresenceRequired);

        // Vérification
        try {
            webAuthnManager.verify(registrationData, registrationParameters);
        } catch (VerificationException e) {
            throw new IllegalStateException("WebAuthn registration verification failed: " + e.getMessage(), e);
        }

        // persist CredentialRecord object, which will be used in the authentication process.
        assert registrationData.getAttestationObject() != null;
        WebAuthnCredential webAuthnCredential = new WebAuthnCredential(
                user,
                registrationData
        );
        credentialRepository.save(webAuthnCredential);

        // Détruire le challenge (one-time use)
        regChallengeService.invalidateChallenge(attempt);

        // Passer l'utilisateur courant en PENDING s'il est INACTIVE
        if (user.getStatus() == null || user.getStatus() == UserStatus.INACTIVE) {
            user.setStatus(UserStatus.PENDING);
            userRepository.save(user);
        }

        // Créer un JWT avec le status PENDING
        String jwt = jwtService.generateToken(user);

        return new RegisterFinishResponse(jwt, user.getStatus().name());
    }
}
