package fr.siovision.voyages.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.util.Base64;

@Slf4j
@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret-key}")
    private String jwtSecretKey;

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecretKey);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) for HS256.");
            }
            return new ImmutableSecret<>(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("The configured JWT secret-key is not valid Base64.", e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}