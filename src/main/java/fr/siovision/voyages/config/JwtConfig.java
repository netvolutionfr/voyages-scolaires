package fr.siovision.voyages.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class JwtConfig {

    @Value("${app.jwt.private-key}")
    private String jwtPrivateKeyPem;

    @Value("${app.jwt.public-key}")
    private String jwtPublicKeyPem;

    @Bean
    public ECKey ecKey() {
        try {
            ECPrivateKey privateKey = loadPrivateKey(jwtPrivateKeyPem);
            ECPublicKey publicKey = loadPublicKey(jwtPublicKeyPem);

            ECKey ecKey = new ECKey.Builder(Curve.P_256, publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.ES256)
                    .keyIDFromThumbprint()
                    .build();

            log.info("JWT ES256 key loaded. kid={}", ecKey.getKeyID());
            return ecKey;

        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to load JWT EC key pair — check JWT_PRIVATE_KEY and JWT_PUBLIC_KEY", e);
        }
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(ECKey ecKey) {
        return new ImmutableJWKSet<>(new JWKSet(ecKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    private static ECPrivateKey loadPrivateKey(String pem) throws Exception {
        byte[] der = decodePem(pem);
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static ECPublicKey loadPublicKey(String pem) throws Exception {
        byte[] der = decodePem(pem);
        return (ECPublicKey) KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    /** Handles both literal \n (from env vars) and real newlines. */
    private static byte[] decodePem(String pem) {
        String normalized = pem.replace("\\n", "\n");
        String body = normalized.lines()
                .filter(line -> !line.startsWith("-----"))
                .collect(Collectors.joining());
        return Base64.getDecoder().decode(body);
    }
}
