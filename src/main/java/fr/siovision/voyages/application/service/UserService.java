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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private static final java.util.List<UserRole> PRIORITY = java.util.List.of(
            UserRole.ADMIN, UserRole.TEACHER, UserRole.PARENT, UserRole.STUDENT
    );

    private UserRole extractRole(Jwt jwt) {
        java.util.Set<String> bag = new java.util.HashSet<>();

        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof java.util.Collection<?> coll) {
            for (Object r : coll) bag.add(String.valueOf(r).toUpperCase());
        }
        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isBlank()) bag.add(role.toUpperCase());

        for (UserRole candidate : PRIORITY) {
            if (bag.contains(candidate.name())) return candidate;
        }
        return UserRole.UNKNOWN;
    }

    @Transactional
    public UserResponse updateUserTelephone(Jwt jwt, UserTelephoneRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findByPublicId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

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
        return new UserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getFirstName() + " " + user.getLastName(),
                user.getTelephone(),
                user.getStatus() != null ? user.getStatus().name() : "",
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
