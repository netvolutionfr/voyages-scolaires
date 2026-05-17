package fr.siovision.voyages.application.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivationTokenService {

    private final ECKey ecKey;

    @Value("${app.activation.issuer}")
    private String issuer;

    @Value("${app.activation.expiry-minutes}")
    private long expiryMinutes;

    public String generateForUser(String userPublicId, String email) {
        try {
            var now = Instant.now();
            var exp = now.plusSeconds(expiryMinutes * 60);
            var claims = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .issuer(issuer)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .claim("purpose", "activation")
                    .claim("publicId", userPublicId)
                    .subject(email)
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(ecKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();
            var jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(ecKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create activation token", e);
        }
    }

    public ActivationToken verify(String token) {
        try {
            var jwt = SignedJWT.parse(token);
            if (!jwt.verify(new ECDSAVerifier(ecKey.toPublicJWK()))) {
                throw new JOSEException("Invalid signature");
            }

            var claims = jwt.getJWTClaimsSet();
            if (!"activation".equals(claims.getStringClaim("purpose"))) {
                throw new JOSEException("Invalid purpose");
            }
            var now = new Date();
            if (claims.getExpirationTime() == null || now.after(claims.getExpirationTime())) {
                throw new JOSEException("Token expired");
            }
            return new ActivationToken(
                    claims.getStringClaim("publicId"),
                    claims.getSubject()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid activation token", e);
        }
    }

    public record ActivationToken(String publicId, String email) {}
}
