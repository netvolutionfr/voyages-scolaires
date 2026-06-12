package fr.siovision.voyages.config;

import fr.siovision.voyages.application.aspect.RequestAuditFilter;
import fr.siovision.voyages.web.CookieBearerTokenResolver;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // -------- Actuator isolation ----------
    @Order(0)
    @Bean
    SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint());
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    // -------- DEV profile ----------
    @Order(1)
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityChain(HttpSecurity http, RequestAuditFilter audit,
                                                CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/webauthn/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/otp/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/.well-known/jwks.json").permitAll()
                        .requestMatchers("/api/participants/**").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers("/api/files/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/registrations/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/**").hasAnyRole("ADMIN", "PARENT", "STUDENT", "TEACHER")
                )
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(new CookieBearerTokenResolver())
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .addFilterAfter(audit, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    // -------- PROD profile ----------
    @Order(1)
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodSecurityChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/webauthn/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/otp/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/.well-known/jwks.json").permitAll()
                        .requestMatchers("/api/participants/**").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers("/api/files/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/**").hasAnyRole("ADMIN", "PARENT", "STUDENT", "TEACHER")
                )
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(new CookieBearerTokenResolver())
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // HSTS: set by the app when TLS is terminated here; if a reverse proxy
                // terminates TLS, configure HSTS on the proxy and remove this block.
                .headers(h -> h
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                );

        return http.build();
    }

    @Bean
    @Profile("dev")
    public CorsConfigurationSource corsConfigurationSource(FrontProperties front) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                front.url(),
                "http://localhost:3000",
                "http://localhost:8000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) return java.util.Set.of();
            return java.util.Set.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    role.startsWith("ROLE_") ? role : "ROLE_" + role
            ));
        });
        return conv;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            ECKey ecKey,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        try {
            var jwkSource = new ImmutableJWKSet<com.nimbusds.jose.proc.SecurityContext>(
                    new JWKSet(ecKey.toPublicJWK()));

            var keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource);

            var jwtProcessor = new DefaultJWTProcessor<com.nimbusds.jose.proc.SecurityContext>();
            jwtProcessor.setJWSKeySelector(keySelector);

            var decoder = new NimbusJwtDecoder(jwtProcessor);
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
            return decoder;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure JWT decoder", e);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
