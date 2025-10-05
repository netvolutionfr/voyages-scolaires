package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.authentication.AuthResponse;
import fr.siovision.voyages.infrastructure.dto.authentication.AuthnFinishRequest;

public interface AuthenticationService {
    AuthResponse finish(AuthnFinishRequest req, String appOrigin);
}
