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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
    public Page<UserResponse> getAllUsers(Jwt jwt, List<UserRole> roles, Pageable pageable) {
        log.info("Listing users with filter '{}', page: {}, size: {}", roles, pageable.getPageNumber(), pageable.getPageSize());
        // Vérification d'autorisation minimale: ADMIN requis
        if (extractRole(jwt) != UserRole.ADMIN) {
            if (extractRole(jwt) == UserRole.TEACHER) {
                // Récupérer uniquement les utilisateurs de type TEACHER
                Page<User> teachers = userRepository.findByRole(UserRole.TEACHER, pageable);
                return teachers.map(this::toResponse);
            } else {
                log.warn("Tentative d'accès à la liste des utilisateurs par un utilisateur sans rôle ADMIN (Keycloak ID: {}, rôle: {})", jwt.getSubject(), extractRole(jwt));
            }
            throw new AccessDeniedException("Accès refusé: rôle ADMIN requis pour lister les utilisateurs.");
        }

        if (roles == null || roles.isEmpty()) {
            roles = List.of(UserRole.values());
        }
        Page<User> users = userRepository.findByRoleIn(roles, pageable);
        return users
                .map(this::toResponse);
    }

    private UserResponse toResponse(User user) {
        // Validated = si l'utilisateur s'est déjà connecté au moins une fois et a un id Keycloak
        Boolean validated = user.getKeycloakId() != null && !user.getKeycloakId().isBlank();
        return new UserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getFirstName() + " " + user.getLastName(),
                user.getTelephone(),
                validated,
                user.getRole() != null ? user.getRole().name() : UserRole.UNKNOWN.name()
        );
    }

    @Transactional
    public User getUserByJwt(Jwt jwt) {
        UUID userPublicId = jwt.getSubject() != null ? UUID.fromString(jwt.getSubject()) : null;
        return userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userPublicId));
    }
}
