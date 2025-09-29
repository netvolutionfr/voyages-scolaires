package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;

public interface JwtService {
    String generateToken(User user);
}
