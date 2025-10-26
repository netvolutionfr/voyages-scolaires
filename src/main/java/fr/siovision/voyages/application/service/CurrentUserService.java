package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("User not authenticated or invalid token");
        }

        String userId = jwt.getSubject(); // "sub" du JWT = ID utilisateur

        return userRepository.findByPublicId(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new IllegalStateException(
                        "No user found with user ID: " + userId
                ));
    }
}

