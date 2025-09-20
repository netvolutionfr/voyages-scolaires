package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.VoyagePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoyagePreferenceRepository extends JpaRepository<VoyagePreference, Long> {
    Optional<VoyagePreference> findByVoyageIdAndUserPublicId(Long voyageId, UUID publicId);

    long countByVoyageIdAndInterest(Long voyageId, String interest);
}
