package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.RefreshTokenService;
import fr.siovision.voyages.application.service.RotateResult;
import fr.siovision.voyages.domain.model.RefreshToken;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Value("${app.jwt.refresh-ttl-seconds:2592000}")
    private int refreshTokenTtlSeconds;

    public Duration getRefreshTtl() {
        return Duration.ofSeconds(refreshTokenTtlSeconds);
    }

    private final RefreshTokenRepository repo;

    /** Génère un token opaque (Base64URL) de 48 octets (~384 bits). */
    public String generateOpaqueToken() {
        var rnd = new java.security.SecureRandom();
        var buf = new byte[48];
        rnd.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /** SHA-256(token) → byte[] pour stockage en DB. */
    public byte[] sha256(String token) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Création initiale d’un refresh pour un user (login). Retourne le token brut à remettre au client. */
    @Transactional
    public String issue(User user) {
        var raw = generateOpaqueToken();
        var hash = sha256(raw);

        var rt = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(Instant.now().plus(getRefreshTtl()))
                .status("ACTIVE")
                .build();

        // familyId est généré dans l’entity (UUID.randomUUID())
        repo.save(rt);
        return raw;
    }

    /** Rotation : consomme un refresh et en émet un nouveau (renvoie le nouveau token brut). */
    @Transactional
    public RotateResult rotate(String presentedRefresh) {
        var h = sha256(presentedRefresh);

        // Lock pessimiste pour éviter double utilisation concurrente
        var rt = repo.findByHashForUpdate(h).orElseThrow(() -> unauthorized("invalid_refresh"));

        // Vérifs de sécurité
        var now = Instant.now();
        if (!"ACTIVE".equals(rt.getStatus())) {
            // Réutilisation d’un token déjà consommé → révoquer toute la famille
            repo.revokeFamily(rt.getFamilyId());
            throw unauthorized("reused_refresh_revoked_family");
        }
        if (rt.getExpiresAt().isBefore(now)) {
            rt.setStatus("REVOKED");
            repo.save(rt);
            throw unauthorized("expired_refresh");
        }

        // Marque l’ancien comme ROTATED
        rt.setStatus("ROTATED");
        rt.setLastUsedAt(now);
        repo.save(rt);

        // Émet le nouveau (même famille)
        var rawNew = generateOpaqueToken();
        var hashNew = sha256(rawNew);

        var newRt = RefreshToken.builder()
                .user(rt.getUser())
                .tokenHash(hashNew)
                .expiresAt(now.plus(getRefreshTtl()))
                .status("ACTIVE")
                .build();
        newRt.setFamilyId(rt.getFamilyId()); // même famille
        repo.save(newRt);

        // chaînage (optionnel) pour audit
        rt.setReplacedBy(newRt);
        repo.save(rt);

        return new RotateResult(rt.getUser(), rawNew);
    }

    /** Révoque le refresh présenté (logout de l’appareil courant). */
    @Transactional
    public void revoke(String presentedRefresh) {
        var h = sha256(presentedRefresh);
        var rt = repo.findByHashForUpdate(h).orElseThrow(() -> unauthorized("invalid_refresh"));
        if (!"REVOKED".equals(rt.getStatus())) {
            rt.setStatus("REVOKED");
            repo.save(rt);
        }
    }

    /** Révocation de toute la famille (logout-all). */
    @Transactional
    public int revokeFamily(UUID familyId) {
        return repo.revokeFamily(familyId);
    }

    /** Nettoyage périodique des tokens expirés */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public int purgeExpired() {
        return repo.deleteAllExpired(Instant.now());
    }

    private static RuntimeException unauthorized(String reason) {
        return new RuntimeException("unauthorized:" + reason);
    }

}
