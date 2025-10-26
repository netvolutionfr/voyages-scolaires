package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Section;
import fr.siovision.voyages.domain.model.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TripRepository extends JpaRepository<Trip, Long> {
    Page<Trip> findBySectionsContaining(Section userSection, Pageable pageable);
}
