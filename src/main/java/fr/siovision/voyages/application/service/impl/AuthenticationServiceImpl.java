package fr.siovision.voyages.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webauthn4j.WebAuthnAuthenticationManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionsAuthenticatorOutputs;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;
import fr.siovision.voyages.application.service.AuthenticationService;
import fr.siovision.voyages.application.service.ChallengeService;
import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.application.service.RefreshTokenService;
import fr.siovision.voyages.domain.model.RegistrationAttempt;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserStatus;
import fr.siovision.voyages.domain.model.WebAuthnCredential;
import fr.siovision.voyages.infrastructure.dto.authentication.*;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import fr.siovision.voyages.infrastructure.repository.WebAuthnCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl  implements AuthenticationService {
    private final WebAuthnCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final ChallengeService challengeService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final WebAuthnAuthenticationManager webAuthnManager = new WebAuthnAuthenticationManager();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${webauthn.allowed-origins}")          // ex: https://app.campusaway.fr (ou http://localhost:5173 en dev)
    private List<String> allowedOrigins;

    @Value("${webauthn.rp.id}")       // ex: campusaway.fr (ou localhost en dev)
    private String rpId;

    @Value("${webauthn.default-origin}") // ex: https://campusaway.fr
    private String defaultOrigin;

    @Value("${app.jwt.access-ttl-seconds}")
    long accessTtlSec;

    @Value("${app.jwt.refresh-ttl-seconds}")
    long refreshTtlSec;

    @Override
    public AuthResponse finish(AuthnFinishRequest req, String appOrigin) {
        // 1. Parser la requete d'authentification
        String json;
        try {
            json = objectMapper.writeValueAsString(req);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        AuthenticationData authenticationData = webAuthnManager.parse(json);

        // 2. Construire ServerProperties
        // Récupérer le challenge dans registration_attempt
        assert authenticationData.getCollectedClientData() != null;
        Challenge expectedChallenge = authenticationData.getCollectedClientData().getChallenge();
        RegistrationAttempt attempt = challengeService.getChallenge(expectedChallenge)
                .orElseThrow(() -> new IllegalStateException("Challenge d'authentification introuvable"));


        String originValue;

        if (!allowedOrigins.contains(appOrigin)) {
            originValue = defaultOrigin;
        } else {
            originValue = appOrigin;
        }

        Origin origin = new Origin(originValue);
        String rpId = this.rpId;
        ServerProperty serverProperty = new ServerProperty(origin, rpId, expectedChallenge);

        // 3. Expectations
        // Récupérer le credential stocké et construire le contexte de validation
        WebAuthnCredential credential = credentialRepository.findByCredentialId(authenticationData.getCredentialId())
                .orElseThrow(() -> new IllegalStateException("Unknown credential ID: " + Arrays.toString(authenticationData.getCredentialId())));

        // expectations
        // Récupérer le credential dans la base en fonction de ce que l'authentificateur a envoyé
        boolean userVerificationRequired = true; // toujours true Sécurité maximale. L'authentificateur doit vérifier l'utilisateur (biométrie/PIN)
        boolean userPresenceRequired = true; // demande à l'authentificateur de prouver que l'utilisateur est physiquement présent et interagit avec le dispositif au moment de l'opération


        ObjectConverter objectConverter = new ObjectConverter();
        var cose = objectConverter.getCborConverter().readValue(credential.getCoseKey(), COSEKey.class);
        AAGUID aaguid = new AAGUID(UUID.fromString(credential.getAaguid()));
        CredentialRecord credentialRecord = getCredentialRecord(cose, aaguid, credential);

        AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                serverProperty,
                credentialRecord,
                null,
                userVerificationRequired,
                userPresenceRequired
        );
        // Vérification
        try {
            webAuthnManager.verify(authenticationData, authenticationParameters);
        } catch (VerificationException e) {
            throw new IllegalStateException("WebAuthn authentication verification failed: " + e.getMessage(), e);
        }

        // Détruire le challenge (one-time use)
        challengeService.invalidateChallenge(attempt);

        byte[] userHandle = authenticationData.getUserHandle();
        UUID userPublicId = UUID.nameUUIDFromBytes(userHandle);

        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalStateException("User not found for UUID: " + userPublicId));

        // Créer un JWT
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .expiresIn(accessTtlSec)
                .refreshToken(refreshToken)
                .refreshExpiresIn(refreshTtlSec)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getPublicId().toString())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getStatus() == UserStatus.PENDING ? "PENDING" : user.getRole().toString())
                        .status(user.getStatus().name())
                        .build())
                .build();
    }

    private static CredentialRecord getCredentialRecord(COSEKey cose, AAGUID aaguid, WebAuthnCredential credential) {
        assert cose != null;
        var attested = new AttestedCredentialData(
                aaguid,
                credential.getCredentialId(),
                cose);
        return new CredentialRecordImpl(
                /*
                    @NotNull AttestationStatement attestationStatement,
                    @Nullable Boolean uvInitialized,
                    @Nullable Boolean backupEligible,
                    @Nullable Boolean backupState,
                    long counter,
                    @NotNull AttestedCredentialData attestedCredentialData,
                    @NotNull AuthenticationExtensionsAuthenticatorOutputs<RegistrationExtensionAuthenticatorOutput> authenticatorExtensions,
                    @Nullable CollectedClientData clientData,
                    @Nullable AuthenticationExtensionsClientOutputs<RegistrationExtensionClientOutput> clientExtensions,
                    @Nullable Set<AuthenticatorTransport> transports
                 */
                null,
                null,
                null,
                null,
                credential.getSignatureCount(),
                attested,
                new AuthenticationExtensionsAuthenticatorOutputs<>(),
                null,
                null,
                null
        );
    }
}
