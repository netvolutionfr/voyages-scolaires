package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.RectificationRequest;
import fr.siovision.voyages.domain.model.RectificationStatus;
import fr.siovision.voyages.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RectificationRequestRepository extends JpaRepository<RectificationRequest, UUID> {
    Page<RectificationRequest> findByStatus(RectificationStatus status, Pageable pageable);

    // Effacement RGPD
    @Modifying
    @Query("delete from RectificationRequest r where r.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("update RectificationRequest r set r.processedBy = null where r.processedBy = :user")
    void clearProcessedBy(@Param("user") User user);
}
