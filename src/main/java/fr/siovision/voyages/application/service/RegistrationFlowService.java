package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

public interface RegistrationFlowService {
    RegisterFinishResponse finish(@Valid RegisterFinishRequest req);
}
