package fr.siovision.voyages.application.service.impl;

import fr.siovision.voyages.application.service.JwtService;
import fr.siovision.voyages.application.service.MailService;
import fr.siovision.voyages.application.service.OtpService;
import fr.siovision.voyages.application.service.RefreshTokenService;
import fr.siovision.voyages.domain.model.OtpToken;
import fr.siovision.voyages.domain.model.RefreshToken;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.authentication.RefreshResponse;
import fr.siovision.voyages.infrastructure.repository.OtpTokenRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final OtpTokenRepository otpRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.ttl-minutes:10}")
    private long otpTtlMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.jwt.access-ttl-seconds:900}")
    private long accessTtlSeconds;

    @Value("${app.jwt.refresh-ttl-seconds:2592000}")
    private long refreshTtlSeconds;

    private static final SecureRandom SR = new SecureRandom();

    @Override
    @Transactional
    public void issueAndSend(User user) {
        if (user == null) throw new IllegalArgumentException("user cannot be null");
        // Anti-spam: respect du cooldown si un OTP PENDING existe déjà
        var existing = otpRepo.findOtpTokenByUserAndPurposeAndStatusOrderByCreatedAtDesc(
                user, OtpToken.Purpose.ACCOUNT_VERIFICATION, OtpToken.Status.PENDING
        );

        if (existing.isPresent()) {
            var last = existing.get();
            var permissible = last.getCreatedAt().plus(resendCooldownSeconds, ChronoUnit.SECONDS);
            if (Instant.now().isBefore(permissible)) {
                // On ne renvoie pas un nouveau code trop vite ; on peut relancer le même e-mail (optionnel)
                // Ici, on choisit de REFUSER pour éviter l’abus :
                throw new TooManyRequestsException("Veuillez patienter avant de redemander un code.");
            }
            // Optionnel: marquer l'ancien PENDING comme EXPIRED avant d'en créer un nouveau
            last.setStatus(OtpToken.Status.EXPIRED);
        }

        String plainCode = generateNumericOtp(otpLength); // p.ex. "482193"
        String hash = passwordEncoder.encode(plainCode);

        OtpToken token = OtpToken.builder()
                .user(user)
                .purpose(OtpToken.Purpose.ACCOUNT_VERIFICATION)
                .codeHash(hash)
                .expiresAt(Instant.now().plus(otpTtlMinutes, ChronoUnit.MINUTES))
                .status(OtpToken.Status.PENDING)
                .attempts(0)
                .build();

        otpRepo.save(token);

        // Envoi e-mail
        mailService.sendOtpEmail(user, plainCode, otpTtlMinutes);
    }

    @Override
    @Transactional
    public RefreshResponse verifyAccountOtp(String email, String otpCode) {
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        OtpToken token = otpRepo.findOtpTokenByUserAndPurposeAndStatusOrderByCreatedAtDesc(
                        user, OtpToken.Purpose.ACCOUNT_VERIFICATION, OtpToken.Status.PENDING)
                .orElseThrow(() -> new InvalidOtpException("Aucun OTP actif. Demandez un nouveau code."));

        if (Instant.now().isAfter(token.getExpiresAt())) {
            token.setStatus(OtpToken.Status.EXPIRED);
            throw new InvalidOtpException("Code expiré. Demandez un nouveau code.");
        }

        if (token.getAttempts() >= maxAttempts) {
            token.setStatus(OtpToken.Status.EXPIRED);
            throw new InvalidOtpException("Nombre maximum de tentatives atteint. Demandez un nouveau code.");
        }

        token.setAttempts(token.getAttempts() + 1);

        boolean ok = passwordEncoder.matches(otpCode, token.getCodeHash());
        if (!ok) {
            throw new InvalidOtpException("Code invalide.");
        }

        // Succès: consommer l’OTP
        token.setStatus(OtpToken.Status.USED);
        token.setConsumedAt(Instant.now());
        otpRepo.save(token);

        // Logique métier : activer l’utilisateur (ex: passer de PENDING -> ACTIVE)
        user.markAsVerified();
        userRepo.save(user);

        String newAccessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.generateOpaqueToken();

        return new RefreshResponse(
                "Bearer",
                newAccessToken,
                accessTtlSeconds,
                refreshToken,
                refreshTtlSeconds
        );
    }

    private static String generateNumericOtp(int length) {
        // Sans biais modulo : on tire des chiffres 0-9 uniformément
        char[] digits = new char[length];
        for (int i = 0; i < length; i++) {
            int n;
            // 0..9 uniformes : rejeter > 249 pour garder une proba uniforme sur 0..9 (25*10=250)
            do { n = SR.nextInt(256); } while (n > 249);
            digits[i] = (char) ('0' + (n % 10));
        }
        return new String(digits);
    }

    // Exceptions dédiées
    public static class InvalidOtpException extends RuntimeException {
        public InvalidOtpException(String msg) { super(msg); }
    }
    public static class TooManyRequestsException extends RuntimeException {
        public TooManyRequestsException(String msg) { super(msg); }
    }
}
