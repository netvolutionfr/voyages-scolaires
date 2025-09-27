package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.WebAuthnRegistrationService;
import fr.siovision.voyages.infrastructure.dto.authentication.VerifiedCredential;
import org.springframework.stereotype.Service;

@Service
public class WebAuthnRegistrationServiceImpl implements WebAuthnRegistrationService {
    @Override
    public VerifiedCredential verifyAttestation(String clientDataJSONb64u,
                                         String attestationObjectB64u,
                                         String expectedChallengeB64u,
                                         String rpId, String origin) {

        /* TODO: Implement WebAuthn attestation verification logic here */
        return null;
    }
}