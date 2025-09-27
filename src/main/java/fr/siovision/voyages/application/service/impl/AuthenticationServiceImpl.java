package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.AuthenticationService;
import fr.siovision.voyages.infrastructure.dto.authentication.AuthnFinishRequest;
import fr.siovision.voyages.infrastructure.dto.authentication.AuthnOptionsResponse;
import fr.siovision.voyages.infrastructure.dto.authentication.EmailHint;
import fr.siovision.voyages.infrastructure.dto.authentication.JwtResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl  implements AuthenticationService {
    @Override
    public AuthnOptionsResponse options(EmailHint req) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JwtResponse finish(@Valid AuthnFinishRequest req) {
        // TODO Auto-generated method stub
        return null;
    }
}
