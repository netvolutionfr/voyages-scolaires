package fr.siovision.voyages.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        // Instantiate the converter exactly as SecurityConfig does, without Spring context
        SecurityConfig config = new SecurityConfig();
        converter = config.jwtAuthenticationConverter();
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "ES256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void jwt_withRoleAdminClaim_producesRoleAdminAuthority() {
        Jwt jwt = buildJwt(Map.of("role", "ADMIN"));

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void jwt_withRolesClaim_producesNoAuthority() {
        // "roles" (plural) is NOT the claim the converter reads — it reads "role" (singular)
        Jwt jwt = buildJwt(Map.of("roles", java.util.List.of("ADMIN")));

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void jwt_withNoRoleClaim_producesNoAuthority() {
        Jwt jwt = buildJwt(Map.of("sub", "user@example.com"));

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void jwt_withRoleAlreadyPrefixed_doesNotAddDoublePrefix() {
        Jwt jwt = buildJwt(Map.of("role", "ROLE_TEACHER"));

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TEACHER");

        // Verify no double prefix
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .doesNotContain("ROLE_ROLE_TEACHER");
    }

    @Test
    void jwt_withBlankRoleClaim_producesNoAuthority() {
        Jwt jwt = buildJwt(Map.of("role", "  "));

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void jwt_withRoleStudent_producesRoleStudentAuthority() {
        Jwt jwt = buildJwt(Map.of("role", "STUDENT"));

        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // JwtAuthenticationConverter exposes the authorities converter which we invoke indirectly.
        // We call convert() and extract grantedAuthorities from the resulting authentication.
        var auth = converter.convert(jwt);
        return (Collection<GrantedAuthority>) auth.getAuthorities();
    }
}
