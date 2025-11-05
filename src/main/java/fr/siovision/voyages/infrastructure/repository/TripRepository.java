package fr.siovision.voyages.infrastructure.repository;

import aj.org.objectweb.asm.commons.Remapper;
import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.domain.model.Trip;
import fr.siovision.voyages.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TripRepository extends JpaRepository<Trip, Long> {
    Page<Trip> findBySectionsContaining(Section userSection, Pageable pageable);

    Page<Trip> findByChaperonesContaining(User currentUser, Pageable pageable);
}
