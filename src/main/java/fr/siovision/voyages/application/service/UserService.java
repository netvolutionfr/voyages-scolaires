package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.UserCreateRequest;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import fr.siovision.voyages.infrastructure.dto.UserTelephoneRequest;
import fr.siovision.voyages.infrastructure.dto.UserUpdateRequest;
import fr.siovision.voyages.infrastructure.mapper.UserMapper;
import fr.siovision.voyages.infrastructure.repository.SectionRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final java.util.List<UserRole> PRIORITY = java.util.List.of(
            UserRole.ADMIN, UserRole.TEACHER, UserRole.PARENT, UserRole.STUDENT
    );
    private final SectionRepository sectionRepository;

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

        return userMapper.toDTO(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Jwt jwt, List<UserRole> roles, Pageable pageable) {
        // Vérification d'autorisation minimale: ADMIN requis
        if (extractRole(jwt) != UserRole.ADMIN) {
            if (extractRole(jwt) == UserRole.TEACHER) {
                // Récupérer uniquement les utilisateurs de type TEACHER
                Page<User> teachers = userRepository.findByRole(UserRole.TEACHER, pageable);
                return teachers.map(userMapper::toDTO);
            } else {
                log.warn("Tentative d'accès à la liste des utilisateurs par un utilisateur sans rôle ADMIN (User ID: {}, rôle: {})", jwt.getSubject(), extractRole(jwt));
            }
            throw new AccessDeniedException("Accès refusé: rôle ADMIN requis pour lister les utilisateurs.");
        }

        if (roles == null || roles.isEmpty()) {
            roles = List.of(UserRole.values());
        }
        Page<User> users = userRepository.findByRoleIn(roles, pageable);
        return users
                .map(userMapper::toDTO);
    }


    @Transactional
    public User getUserByJwt(Jwt jwt) {
        UUID userPublicId = jwt.getSubject() != null ? UUID.fromString(jwt.getSubject()) : null;
        return userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userPublicId));
    }

    public User getUserByPublicId(Jwt jwt, String userPublicId) {
        // Vérification d'autorisation minimale: ADMIN requis
        if (extractRole(jwt) != UserRole.ADMIN) {
            log.warn("Tentative d'accès à un utilisateur par un utilisateur sans rôle ADMIN (User ID: {}, rôle: {})", jwt.getSubject(), extractRole(jwt));
            throw new AccessDeniedException("Accès refusé: rôle ADMIN requis pour accéder aux détails de l'utilisateur.");
        }

        UUID publicId = UUID.fromString(userPublicId);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userPublicId));
        log.info("Consultation des détails de l'utilisateur {} par administrateur {}", user.getEmail(), jwt.getSubject());
        return user;
    }

    public UserResponse updateUser(Jwt jwt, String userPublicId, UserUpdateRequest request) {
        // Vérification d'autorisation minimale: ADMIN requis
        if (extractRole(jwt) != UserRole.ADMIN) {
            log.warn("Tentative de mise à jour d'un utilisateur par un utilisateur sans rôle ADMIN (User ID: {}, rôle: {})", jwt.getSubject(), extractRole(jwt));
            throw new AccessDeniedException("Accès refusé: rôle ADMIN requis pour mettre à jour les détails de l'utilisateur.");
        }

        UUID publicId = UUID.fromString(userPublicId);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userPublicId));

        // Mettre à jour les champs modifiables
        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getSectionPublicId() != null) {
            Section section = sectionRepository.findByPublicId(request.getSectionPublicId())
                    .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + request.getSectionPublicId()));
            user.setSection(section);
        }
        if (request.getBirthDate() != null) {
            try {
                user.setBirthDate(LocalDate.parse(request.getBirthDate().trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid birth date format: " + request.getBirthDate());
            }
        }
        if (request.getGender() != null) {
            if (request.getGender().equals("M") || request.getGender().equals("F") || request.getGender().equals("N")) {
                user.setGender(request.getGender());
            } else {
                throw new IllegalArgumentException("Invalid gender value: " + request.getGender());
            }
        }

        userRepository.save(user);
        log.info("Mise à jour des détails de l'utilisateur {} par administrateur {}", user.getEmail(), jwt.getSubject());
        return userMapper.toDTO(user);
    }

    public User createUser(Jwt jwt, UserCreateRequest request) {
        // Vérification d'autorisation minimale: ADMIN requis
        if (extractRole(jwt) != UserRole.ADMIN) {
            log.warn("Tentative de création d'un utilisateur par un utilisateur sans rôle ADMIN (User ID: {}, rôle: {})", jwt.getSubject(), extractRole(jwt));
            throw new AccessDeniedException("Accès refusé: rôle ADMIN requis pour créer un utilisateur.");
        }

        User user = new User();
        // Check if email is empty
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is mandatory");
        }

        // Email is unique and mandatory
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setTelephone(request.getTelephone());

        if (request.getSectionPublicId() != null) {
            Section section = sectionRepository.findByPublicId(request.getSectionPublicId())
                    .orElseThrow(() -> new IllegalArgumentException("Section not found with id: " + request.getSectionPublicId()));
            user.setSection(section);
        }
        if (request.getBirthDate() != null) {
            try {
                user.setBirthDate(LocalDate.parse(request.getBirthDate().trim()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid birth date format: " + request.getBirthDate());
            }
        }
        if (request.getGender() != null) {
            if (request.getGender().equals("M") || request.getGender().equals("F") || request.getGender().equals("N")) {
                user.setGender(request.getGender());
            } else {
                throw new IllegalArgumentException("Invalid gender value: " + request.getGender());
            }
        }
        user.setRole(request.getRole());

        userRepository.save(user);
        log.info("Création du nouvel utilisateur {} par administrateur {}", user.getEmail(), jwt.getSubject());
        return user;
    }

    public void deleteUser(Jwt jwt, String userPublicId) {
        // Vérification d'autorisation minimale: ADMIN requis
        if (extractRole(jwt) != UserRole.ADMIN) {
            log.warn("Tentative de suppression d'un utilisateur par un utilisateur sans rôle ADMIN (User ID: {}, rôle: {})", jwt.getSubject(), extractRole(jwt));
            throw new AccessDeniedException("Accès refusé: rôle ADMIN requis pour supprimer un utilisateur.");
        }

        UUID publicId = UUID.fromString(userPublicId);
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userPublicId));

        userRepository.delete(user);
        log.info("Suppression de l'utilisateur {} par administrateur {}", user.getEmail(), jwt.getSubject());
    }
}
