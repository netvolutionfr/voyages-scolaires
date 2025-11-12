package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.TripUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TripDocumentsAdminRepository extends JpaRepository<TripUser, Long> {

    /** Nombre de types requis pour le voyage. */
    @Query(value = """
        SELECT COUNT(*)
        FROM trip_formality tf
        WHERE tf.trip_id = :tripId
          AND tf.required = TRUE
        """, nativeQuery = true)
    int countRequiredForTrip(@Param("tripId") Long tripId);

    @Query("""
        select u.publicId as userPublicId,
               count(distinct dt.id) as providedCount
        from Document d
            join d.user u
            join d.documentType dt
        where u.publicId in :userPublicIds
          and dt in (
              select f.documentType
              from TripFormality f
              where f.trip.id = :tripId
                and f.required = true
          )
        group by u.publicId
        """)
    List<ProvidedCountRow> countProvidedByUserPublicIdsForTrip(
            @Param("tripId") Long tripId,
            @Param("userPublicIds") Collection<UUID> userPublicIds
    );

    /** Nombre de types fournis (distinct) par user parmi les types requis du voyage. */
    interface ProvidedCountRow {
        Long getUserId();
        Integer getProvidedCount();

        Object getUserPublicId();
    }
}