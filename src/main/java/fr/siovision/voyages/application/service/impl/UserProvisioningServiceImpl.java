package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.UserProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class UserProvisioningServiceImpl implements UserProvisioningService {

    private final UserRepository userRepository;

    @Override
    public User upsertFromKeycloak(
            String keycloakId,
            String email,
            String nom,
            String prenom,
            UserRole preferredRole
    ) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("keycloakId (sub) is required");
        }
        final UserRole incoming = (preferredRole == null) ? UserRole.UNKNOWN : preferredRole;

        return userRepository.findByKeycloakId(keycloakId)
                .map(existing -> updateIfNeeded(existing, email, nom, prenom, incoming))
                .orElseGet(() -> createNew(keycloakId, email, nom, prenom, incoming));
    }

    private User updateIfNeeded(User u, String email, String nom, String prenom, UserRole incoming) {
        boolean dirty = false;

        if (email != null && !email.equals(u.getEmail())) {
            u.setEmail(email);
            dirty = true;
        }
        if (nom != null && !nom.equals(u.getNom())) {
            u.setNom(cap(nom));
            dirty = true;
        }
        if (prenom != null && !prenom.equals(u.getPrenom())) {
            u.setPrenom(cap(prenom));
            dirty = true;
        }

        if (shouldUpgrade(u.getRole(), incoming)) {
            u.setRole(incoming);
            dirty = true;
        }

        if (dirty) {
            u.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(u);
        }
        return u;
    }

    private User createNew(String keycloakId, String email, String nom, String prenom, UserRole incoming) {
        User u = new User();
        u.setKeycloakId(keycloakId);
        u.setEmail(email);
        u.setNom(cap(nom));
        u.setPrenom(cap(prenom));
        u.setRole(incoming == null ? UserRole.UNKNOWN : incoming);
        u.setCreatedAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    private boolean shouldUpgrade(UserRole current, UserRole incoming) {
        if (incoming == null || incoming == UserRole.UNKNOWN) return false;
        if (current == null || current == UserRole.UNKNOWN) return true;
        return rank(incoming) < rank(current);
    }

    private int rank(UserRole r) {
        return switch (r) {
            case ADMIN -> 0;
            case TEACHER -> 1;
            case STUDENT -> 2;
            case PARENT -> 3;
            case UNKNOWN -> 4;
        };
    }

    private String cap(String s) {
        if (s == null || s.isBlank()) return s;
        s = s.trim();
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
