package fr.siovision.voyages.application.service.impl;

import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import fr.siovision.voyages.application.service.ChallengeService;
import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.application.service.RegistrationFlowService;
import fr.siovision.voyages.domain.model.UserStatus;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishResponse;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import fr.siovision.voyages.infrastructure.repository.WebAuthnCredentialRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.webauthn4j.WebAuthnRegistrationManager;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.data.client.challenge.Challenge;

import java.util.*;

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

    @Value("${app.origin}")          // ex: https://app.campusaway.fr (ou http://localhost:5173 en dev)
    private String appOrigin;

    @Value("${app.rp.id}")       // ex: campusaway.fr (ou localhost en dev)
    private String rpId;

    @Override
    public RegisterFinishResponse finishRegistration(RegisterFinishRequest req) {
        // 1) Vérifier que l'email est en base
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2) Mettre/laisser le user en PENDING (le compte sera activé après vérif email)
        if (user.getStatus() == null || user.getStatus() == UserStatus.INACTIVE) {
            user.setStatus(UserStatus.PENDING);
            userRepository.save(user);
        }

        // 3) Vérifier l'attestation WebAuthn
        //    3.1 Récupérer le challenge émis à l’étape /webauthn/register/options
        //        Selon ta clé d’indexation : par email, par device nonce, etc.
        byte[] myChallenge = regChallengeService
                .consumeRegistrationChallengeFor(user.getId(), req.email()); // <= TODO IMPORTANT: consommer/invalider
        Challenge expectedChallenge = new DefaultChallenge(myChallenge);

        //    3.2 Parser la réponse JSON renvoyée par le front
        //        Le front doit t'envoyer la response JSON WebAuthn 'PublicKeyCredential<Attestation>'
        String registrationResponseJSON = req.registrationResponseJson();
        if (registrationResponseJSON == null || registrationResponseJSON.isBlank()) {
            throw new IllegalArgumentException("registrationResponseJson is required");
        }
        RegistrationData registrationData =
                webAuthnManager.parse(registrationResponseJSON);

        //    3.3 Construire les ServerProperties
        ServerProperty serverProperty = new ServerProperty(
                new Origin(appOrigin),
                rpId,
                expectedChallenge
        );

        //    3.4 Construire les paramètres de vérification
        List<PublicKeyCredentialParameters> pubKeyCredParams = null;
        boolean userVerificationRequired = false;
        boolean userPresenceRequired = true;
        RegistrationParameters params = new RegistrationParameters(
                serverProperty,
                pubKeyCredParams,
                userVerificationRequired,
                userPresenceRequired
        );

        //    3.5 Vérifier
        try {
            webAuthnManager.verify(registrationData, params);
        } catch (Exception ex) {
            throw new IllegalStateException("WebAuthn registration verification failed: " + ex.getMessage(), ex);
        }

        // 4) Persister le credential (extraction des champs utiles)
        // registrationData = résultat d’un verify réussi (ex: webAuthnManager.verifyRegistrationResponseJSON(...))

        // please persist CredentialRecord object, which will be used in the authentication process.
        assert registrationData.getAttestationObject() != null;
        CredentialRecord credentialRecord =
                new CredentialRecordImpl( // You may create your own CredentialRecord implementation to save friendly authenticator name
                        registrationData.getAttestationObject(),
                        registrationData.getCollectedClientData(),
                        registrationData.getClientExtensions(),
                        registrationData.getTransports()
                );

        WebAuthnCredential cred = new WebAuthnCredential();
        cred.setUser(user);
        credentialRepository.save(cred);

        // 5) Retour JWT
        String jwt = jwtService.generateToken(user);

        return new RegisterFinishResponse(jwt, user.getStatus().name());
    }

}
