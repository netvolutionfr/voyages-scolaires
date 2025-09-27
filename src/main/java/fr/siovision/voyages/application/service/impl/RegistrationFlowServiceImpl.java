package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.RegistrationFlowService;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.RegisterFinishResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class RegistrationFlowServiceImpl implements RegistrationFlowService {
    @Override
    public RegisterFinishResponse finishRegistration(RegisterFinishRequest req) {
        /* TODO: Implement registration flow finish logic here */
        return null;
    }
}