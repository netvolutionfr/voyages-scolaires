package fr.siovision.voyages.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
    public SecurityFilterChain devSecurityChain(HttpSecurity http) throws Exception {
        http
                // CSRF requis pour /webauthn/* (POST)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/auth/**") // Pas de CSRF sur ces endpoints
                )
                .cors(cors -> corsConfigurer())
                .authorizeHttpRequests(auth -> auth
                        // Passkeys/WebAuthn
                        .requestMatchers("/webauthn/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // Docs
                        .requestMatchers("/swagger-ui/**","/v3/api-docs/**","/swagger-ui.html").permitAll()

                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public
                        .requestMatchers("/api/public/**").permitAll()

                        // Règles métiers existantes
                        .requestMatchers("/api/participants/**").hasAnyRole("ADMIN","PARENT")
                        .requestMatchers("/api/files/**").hasAnyRole("ADMIN","TEACHER")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/**").authenticated()
                )
                // Activation Passkeys (Spring Security)
                .webAuthn(wa -> wa
                                .rpName("Campus Away")
                                .rpId("campusaway.fr")
                        // .allowedOrigins("https://campusaway.fr","https://www.campusaway.fr")
                );

        return http.build();
    }

    @Order(1)
    @Bean
    @Profile("dev")
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();

        // Nouvelle méthode pour configurer le cookie de manière personnalisée
        repository.setCookieCustomizer(builder ->
                builder
                        .sameSite("None") // Définition de la politique SameSite
                        .secure(true)     // Requis avec SameSite=None
                        .httpOnly(false)  // Permet au JS de lire le cookie
        );

        return repository;
    }

    // -------- PROD profile ----------
    @Order(1)
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodSecurityChain(HttpSecurity http) throws Exception {
        http
                // On laisse CSRF actif (requis pour /webauthn/*).
                // Si ton SPA est même domaine, pas besoin de CookieCsrfTokenRepo lisible.
                .csrf(Customizer.withDefaults())
                .cors(AbstractHttpConfigurer::disable) // géré par le proxy frontal
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webauthn/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**","/v3/api-docs/**","/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/participants/**").hasAnyRole("ADMIN","PARENT")
                        .requestMatchers("/api/files/**").hasAnyRole("ADMIN","TEACHER")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/**").authenticated()
                )
                // Pas de formulaire ni basic auth
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Active la config Passkeys / WebAuthn
                .webAuthn(wa -> wa
                        .rpName("Campus Away")
                        .rpId("campusaway.fr")
                );

        return http.build();
    }

    // -------- CORS DEV ----------
    @Bean
    @Profile("dev")
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173","http://localhost:3000","https://campusaway.fr")
                        .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
