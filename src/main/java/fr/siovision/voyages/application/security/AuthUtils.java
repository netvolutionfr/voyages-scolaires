package fr.siovision.voyages.application.security;

import fr.siovision.voyages.domain.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public final class AuthUtils {
    private AuthUtils() {}

    public static List<SimpleGrantedAuthority> authoritiesFor(User u) {
        // mappe UserRole -> ROLE_*
        return List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
    }
}
