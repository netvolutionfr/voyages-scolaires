package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.RefreshToken;
import fr.siovision.voyages.domain.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // Lookup par hash (binaire) — on lock pour éviter une double conso en concurrence
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.tokenHash = :hash")
    Optional<RefreshToken> findByHashForUpdate(@Param("hash") byte[] hash);

    // Tokens actifs d’un user (utile pour lister les sessions)
    List<RefreshToken> findByUserAndStatus(User user, String status);

    // Révocation de toute la famille
    @Modifying
    @Query("update RefreshToken rt set rt.status='REVOKED' where rt.familyId = :fid and rt.status <> 'REVOKED'")
    int revokeFamily(@Param("fid") UUID familyId);

    // Ménage (optionnel) : supprimer les expirés
    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :now")
    int deleteAllExpired(@Param("now") Instant now);

}
