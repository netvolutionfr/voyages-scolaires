package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.domain.model.UserStatus;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtServiceImpl implements JwtService {
    private final JwtEncoder encoder;

    @Value("${app.jwt.pending-ttl-seconds}")
    long pendingExpiry; // 15 minutes

    @Value("${app.jwt.access-ttl-seconds}")
    long accessExpiry; // 1 heure

    public JwtServiceImpl(JwtEncoder encoder) {
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
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(user.getEmail())
                .claim("role",  user.getRole())
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
