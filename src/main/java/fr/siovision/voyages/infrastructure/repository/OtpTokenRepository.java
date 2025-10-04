package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.OtpToken;
import fr.siovision.voyages.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    long deleteByStatusOrExpiresAtBefore(OtpToken.Status status, Instant cutoff);

    Optional<OtpToken> findOtpTokenByUserAndPurposeAndStatusOrderByCreatedAtDesc(User user, OtpToken.Purpose purpose, OtpToken.Status status);
}