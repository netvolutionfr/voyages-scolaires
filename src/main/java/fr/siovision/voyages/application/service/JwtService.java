package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;

import java.util.List;

public interface JwtService {
    String generateToken(User user);
    String generateAccessToken(User user, List<String> amr);

    default String generateAccessToken(User user) {
        return generateAccessToken(user, List.of("webauthn"));
    }
}
