package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    @Query("SELECT t FROM Trip t WHERE " +
            "t.registrationOpeningDate <= CURRENT_DATE AND " +
            "t.registrationClosingDate >= CURRENT_DATE")
    List<Trip> findAllOpenForRegistration();
}
