package fr.siovision.voyages.application.service;

import com.webauthn4j.data.PublicKeyCredential;
import com.webauthn4j.data.RegistrationRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.AuthnOptionsResponse;
import fr.siovision.voyages.infrastructure.dto.authentication.EmailHint;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishResponse;
import jakarta.validation.Valid;

public interface RegistrationFlowService {
    RegisterFinishResponse finishRegistration(String registrationRequest, String appOrigin);
}
