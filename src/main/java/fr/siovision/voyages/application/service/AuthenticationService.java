package fr.siovision.voyages.application.service;

import com.webauthn4j.data.AuthenticationRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.AuthResponse;
import fr.siovision.voyages.infrastructure.dto.authentication.JwtResponse;

public interface AuthenticationService {
    AuthResponse finish(AuthenticationRequest req, String appOrigin);
}
