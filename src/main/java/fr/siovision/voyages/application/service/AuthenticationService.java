package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.authentication.AuthnFinishRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.AuthnOptionsResponse;
import fr.siovision.voyages.infrastructure.dto.authentication.EmailHint;
import fr.siovision.voyages.infrastructure.dto.authentication.JwtResponse;
import jakarta.validation.Valid;

public interface AuthenticationService {
    JwtResponse finish(AuthnFinishRequest req);
    AuthnOptionsResponse options(EmailHint req);
}
