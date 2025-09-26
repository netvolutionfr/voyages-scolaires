package fr.siovision.voyages.application.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class UserPrincipal implements UserDetails {
    private final String email;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(String email, String role) {
        this.email = email;
        String normalized = (role == null || role.isBlank()) ? "USER" : role.trim().toUpperCase();
        String withPrefix = normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
        this.authorities = List.of(new SimpleGrantedAuthority(withPrefix));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }                 // <<< pas de mot de passe
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
