package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.OtpToken;
import fr.siovision.voyages.domain.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    long deleteByStatusOrExpiresAtBefore(OtpToken.Status status, Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OtpToken o WHERE o.user = :user AND o.purpose = :purpose AND o.status = :status ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpToken> findLatestPendingForUpdate(
            @Param("user") User user,
            @Param("purpose") OtpToken.Purpose purpose,
            @Param("status") OtpToken.Status status);

    long countByUserAndPurposeAndCreatedAtAfter(User user, OtpToken.Purpose purpose, Instant after);
}