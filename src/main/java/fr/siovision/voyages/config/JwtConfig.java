package fr.siovision.voyages.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret-key}")
    private String jwtSecretKey;

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // Crée une clé symétrique à partir de votre secret
        SecretKey key = new SecretKeySpec(jwtSecretKey.getBytes(), "HmacSHA256");
        // Fournit la clé pour l'encodage
        return new ImmutableSecret<>(key);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}