package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Utilisateur non authentifié ou token invalide");
        }

        String keycloakId = jwt.getSubject(); // "sub" du JWT = ID Keycloak

        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun utilisateur trouvé pour keycloakId=" + keycloakId
                ));
    }
}

