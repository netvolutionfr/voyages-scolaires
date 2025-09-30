package fr.siovision.voyages.application.service;

import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.client.challenge.Challenge;
import fr.siovision.voyages.domain.model.RegistrationAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeService {
    public PublicKeyCredentialCreationOptions issue(String email, String rpOrigin);

    Optional<RegistrationAttempt> getChallenge(Challenge myChallenge);

    List<PublicKeyCredentialParameters> getPubKeyCredParams();

    AuthenticatorSelectionCriteria getAuthenticatorSelectionCriteria();

    void invalidateChallenge(RegistrationAttempt attempt);
}