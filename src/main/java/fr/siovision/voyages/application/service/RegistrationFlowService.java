package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishResponse;
import jakarta.validation.Valid;

public interface RegistrationFlowService {
    RegisterFinishResponse finishRegistration(String registrationRequest, String appOrigin);
    RegisterFinishResponse finishRegistrationOneStep(@Valid RegisterFinishRequest registerFinishRequest, String origin);
}
