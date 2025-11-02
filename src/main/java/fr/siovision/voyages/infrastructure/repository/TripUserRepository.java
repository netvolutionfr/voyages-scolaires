package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.TripRegistrationStatus;
import fr.siovision.voyages.domain.model.TripUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TripUserRepository extends JpaRepository<TripUser, Long> {

    // On charge Trip + formalities + formalities.documentType + country d’un coup
    @EntityGraph(attributePaths = {
            "trip",
            "trip.country",
            "trip.formalities",
            "trip.formalities.documentType"
    })
    @Query("""
        select tu
        from TripUser tu
        where tu.user.id = :userId
          and tu.registrationStatus in :activeStatuses
          and tu.trip.departureDate >= :today
        order by tu.trip.departureDate asc
    """)
    List<TripUser> findActiveByUser(
            @Param("userId") Long userId,
            @Param("activeStatuses") Collection<TripRegistrationStatus> activeStatuses,
            @Param("today") LocalDate today
    );

    boolean existsByTrip_IdAndUser_Id(Long tripId, Long userId);
}