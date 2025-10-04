package fr.siovision.voyages.application.service;

import com.webauthn4j.data.AuthenticationRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.JwtResponse;

public interface AuthenticationService {
    JwtResponse finish(AuthenticationRequest req, String appOrigin);
}
