package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.RectificationRequest;
import fr.siovision.voyages.domain.model.RectificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RectificationRequestRepository extends JpaRepository<RectificationRequest, UUID> {
    Page<RectificationRequest> findByStatus(RectificationStatus status, Pageable pageable);
}
