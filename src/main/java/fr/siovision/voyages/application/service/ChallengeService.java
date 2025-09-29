package fr.siovision.voyages.application.service;

import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import fr.siovision.voyages.infrastructure.dto.authentication.ChallengeResponse;

public interface ChallengeService {
    PublicKeyCredentialCreationOptions issue();

    byte[] consumeRegistrationChallengeFor(Long id, String email);
}