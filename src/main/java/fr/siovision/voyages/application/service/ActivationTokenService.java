package fr.siovision.voyages.application.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivationTokenService {

    @Value("${app.activation.jwt-secret}")
    private String secret;

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

            var header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();
            var jwt = new SignedJWT(header, claims);
            var signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create activation token", e);
        }
    }

    public ActivationToken verify(String token) {
        try {
            var jwt = SignedJWT.parse(token);
            var verifier = new MACVerifier(secret.getBytes(StandardCharsets.UTF_8));
            if (!jwt.verify(verifier)) throw new JOSEException("Invalid signature");

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
