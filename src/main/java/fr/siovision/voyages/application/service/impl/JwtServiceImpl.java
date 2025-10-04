package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserStatus;
import fr.siovision.voyages.infrastructure.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {
    private final JwtEncoder encoder;

    @Value("${app.jwt.pending-ttl-seconds}")
    long pendingExpiry;

    @Value("${app.jwt.access-ttl-seconds}")
    long accessExpiry;

    @Value("${app.jwt.refresh-ttl-seconds}")
    long refreshExpiry;

    @Value("${app.jwt.issuer}")
    String issuer;

    // Un générateur de nombres aléatoires cryptographiquement fort
    private static final SecureRandom secureRandom = new SecureRandom();
    // Taille du jeton en octets (e.g., 256 bits = 32 octets)
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public JwtServiceImpl(
            JwtEncoder encoder,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.encoder = encoder;
    }

    @Override
    public String generateToken(User user) {
        Instant now = Instant.now();
        long expiry = (user.getStatus() == UserStatus.PENDING) ? pendingExpiry : accessExpiry;

        // Récupérer le status du User
        // Si PENDING, alors role PENDING
        String role = "";
        UserStatus status = user.getStatus();
        if  (status == UserStatus.PENDING) {
            role = "PENDING";
        } else {
            role = user.getRole().toString();
        }

        // Génération du JWT
        // Construction des 'claims' (payload)
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(user.getPublicId().toString())
                .claim("role",  role)
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .build();

        JwsHeader jws = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(jws, claims)).getTokenValue();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        long ttl = (user.getStatus() == UserStatus.PENDING)
                ? pendingExpiry
                : accessExpiry;

        // rôle effectif
        String role = (user.getStatus() == UserStatus.PENDING)
                ? "PENDING"
                : user.getRole().toString();

        // amr (Authentication Methods Reference) : on précise webauthn
        List<String> amr = List.of("webauthn");

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttl))
                .notBefore(now)
                .subject(user.getPublicId().toString())
                .id(UUID.randomUUID().toString())                 // jti
                .claim("amr", amr)                                // ["webauthn"]
                .claim("auth_time", now.getEpochSecond())         // utile côté client
                .claim("role", role)
                .claim("status", user.getStatus().name())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .build();

        JwsHeader jws = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return encoder.encode(JwtEncoderParameters.from(jws, claims)).getTokenValue();
    }
}
