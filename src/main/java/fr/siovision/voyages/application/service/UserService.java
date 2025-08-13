package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.dto.SectionDTO;
import fr.siovision.voyages.infrastructure.dto.UserResponse;
import fr.siovision.voyages.infrastructure.dto.UserTelephoneRequest;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserResponse getOrCreateUserFromToken(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        User user = userRepository.findByKeycloakId(keycloakId).orElseGet(() -> {
            User newuser = new User();
            newuser.setKeycloakId(keycloakId);
            newuser.setEmail(jwt.getClaimAsString("email"));
            newuser.setPrenom(jwt.getClaimAsString("given_name"));
            newuser.setNom(jwt.getClaimAsString("family_name"));
            newuser.setRole(extractRole(jwt)); // À adapter selon ton token
            return userRepository.save(newuser);
        });
        // Forcer le rôle à partir du token s'il est différent
        if (user.getRole() == null || user.getRole() == UserRole.UNKNOWN || user.getRole() != extractRole(jwt)) {
            user.setRole(extractRole(jwt));
            userRepository.save(user);
        }

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getTelephone(),
                user.getRole().name()
        );
    }

    private UserRole extractRole(Jwt jwt) {
        var roles = jwt.getClaimAsMap("realm_access");
        if (roles != null && roles.containsKey("roles")) {
            List<String> roleList = (List<String>) roles.get("roles");
            if (roleList.contains("admin")) return UserRole.ADMIN;
            if (roleList.contains("teacher")) return UserRole.TEACHER;
            if (roleList.contains("student")) return UserRole.STUDENT;
            if (roleList.contains("parent")) return UserRole.PARENT;
        }
        return UserRole.UNKNOWN;
    }

    public UserResponse updateUserTelephone(Jwt jwt, UserTelephoneRequest request) {
        String keycloakId = jwt.getSubject();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + keycloakId));

        user.setTelephone(request.getTelephone());
        userRepository.save(user);

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getTelephone(),
                user.getRole().name()
        );
    }

    public Page<UserResponse> getAllUsers(Jwt jwt, String q, Pageable pageable) {
        Page<User> users = userRepository.search(q, pageable);
        return users
                .map(user -> new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getTelephone(),
                user.getRole().name()));
    }
}
