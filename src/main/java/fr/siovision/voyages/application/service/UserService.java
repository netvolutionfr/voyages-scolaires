package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import fr.siovision.voyages.infrastructure.dto.UserTelephoneRequest;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse getOrCreateUserFromToken(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        log.info("Authenticating user with Keycloak ID: {}", keycloakId);
        UserRole tokenRole = extractRole(jwt);
        if (tokenRole == UserRole.UNKNOWN) {
            log.warn("User with Keycloak ID {} has no recognized role. Assigning UNKNOWN role.", keycloakId);
        } else {
            log.info("User with Keycloak ID {} has role: {}", keycloakId, tokenRole);
        }

        User user = userRepository.findByKeycloakId(keycloakId).orElseGet(() -> {
            User newuser = new User();
            newuser.setKeycloakId(keycloakId);
            newuser.setEmail(jwt.getClaimAsString("email"));
            newuser.setPrenom(jwt.getClaimAsString("given_name"));
            newuser.setNom(jwt.getClaimAsString("family_name"));
            newuser.setRole(tokenRole);
            log.info("Creating new user: {}", newuser);
            return userRepository.save(newuser);
        });

        log.info("User found or created: {}", user);

        // Mise à jour éventuelle si le rôle a changé (ou est inconnu)
        if (user.getRole() == null || user.getRole() == UserRole.UNKNOWN || user.getRole() != tokenRole) {
            user.setRole(tokenRole);
            user = userRepository.save(user);
        }

        return toResponse(user);
    }

    private UserRole extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if  (realmAccess == null) return UserRole.UNKNOWN;

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) {
            return UserRole.UNKNOWN;
        }


        boolean isAdmin = roles.stream().anyMatch(r -> "admin".equalsIgnoreCase(String.valueOf(r)));
        if (isAdmin) return UserRole.ADMIN;

        boolean isTeacher = roles.stream().anyMatch(r -> "teacher".equalsIgnoreCase(String.valueOf(r)));
        if (isTeacher) return UserRole.TEACHER;

        boolean isStudent = roles.stream().anyMatch(r -> "student".equalsIgnoreCase(String.valueOf(r)));
        if (isStudent) return UserRole.STUDENT;

        boolean isParent = roles.stream().anyMatch(r -> "parent".equalsIgnoreCase(String.valueOf(r)));
        if (isParent) return UserRole.PARENT;

        return UserRole.UNKNOWN;
    }

    @Transactional
    public UserResponse updateUserTelephone(Jwt jwt, UserTelephoneRequest request) {
        String keycloakId = jwt.getSubject();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + keycloakId));

        user.setTelephone(request.getTelephone());
        userRepository.save(user);

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Jwt jwt, String q, Pageable pageable) {
        // Vérification d'autorisation minimale: ADMIN requis
        if (extractRole(jwt) != UserRole.ADMIN) {
            throw new AccessDeniedException("Accès refusé: rôle ADMIN requis pour lister les utilisateurs.");
        }

        Page<User> users = userRepository.search(q, pageable);
        return users
                .map(this::toResponse);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getTelephone(),
                user.getRole() != null ? user.getRole().name() : UserRole.UNKNOWN.name()
        );
    }
}
