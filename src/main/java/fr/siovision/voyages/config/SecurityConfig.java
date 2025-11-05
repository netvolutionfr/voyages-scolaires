package fr.siovision.voyages.config;

import fr.siovision.voyages.application.aspect.RequestAuditFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.crypto.spec.SecretKeySpec;
import java.util.*;

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
    public SecurityFilterChain devSecurityChain(HttpSecurity http, RequestAuditFilter audit) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> corsConfigurer())
                .authorizeHttpRequests(auth -> auth
                        // Passkeys/WebAuthn
                        .requestMatchers("/api/webauthn/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/otp/**").permitAll()

                        // Docs
                        .requestMatchers("/swagger-ui/**","/v3/api-docs/**","/swagger-ui.html").permitAll()

                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public
                        .requestMatchers("/api/public/**").permitAll()

                        // Règles métiers existantes
                        .requestMatchers("/api/participants/**").hasAnyRole("ADMIN","PARENT")
                        .requestMatchers("/api/files/**").hasAnyRole("ADMIN","TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/registrations/**").hasAnyRole("ADMIN","TEACHER")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/**").authenticated()
                )
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(audit, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);

        return http.build();
    }
    // -------- PROD profile ----------
    @Order(1)
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodSecurityChain(HttpSecurity http, RequestAuditFilter audit) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable) // CORS géré par le proxy frontal (Nginx, Apache, autre) ou api dans le même domaine
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/webauthn/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/otp/**").permitAll()
                        .requestMatchers("/swagger-ui/**","/v3/api-docs/**","/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/participants/**").hasAnyRole("ADMIN","PARENT")
                        .requestMatchers("/api/files/**").hasAnyRole("ADMIN","TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/**").authenticated()
                )
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(audit, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.Set<org.springframework.security.core.GrantedAuthority> out = new java.util.HashSet<>();

            // roles: ["ADMIN", "TEACHER", ...]
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim instanceof java.util.Collection<?> coll) {
                for (Object r : coll) {
                    String role = String.valueOf(r);
                    out.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            role.startsWith("ROLE_") ? role : "ROLE_" + role
                    ));
                }
            }

            // role: "ADMIN"
            String role = jwt.getClaimAsString("role");
            if (role != null && !role.isBlank()) {
                out.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        role.startsWith("ROLE_") ? role : "ROLE_" + role
                ));
            }

            return out;
        });
        return conv;
    }

    @Bean
    public JwtDecoder jwtDecoder(
        @Value("${app.jwt.secret-key}") String secret
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);

        var key = new SecretKeySpec(keyBytes, "HmacSHA256");

        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)  // ton header alg = HS256
                .build();
    }

    @Bean
    @Profile("dev")
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:3000",
                                "http://localhost:5173",
                                "http://localhost:8000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 10 par défaut (ok). Monter à 12 si CPU le permet.
        return new BCryptPasswordEncoder();
    }
}
