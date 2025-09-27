package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.authentication.VerifiedCredential;

public interface WebAuthnRegistrationService {
    VerifiedCredential verifyAttestation(String clientDataJSONb64u,
                                         String attestationObjectB64u,
                                         String expectedChallengeB64u,
                                         String rpId, String origin);
}