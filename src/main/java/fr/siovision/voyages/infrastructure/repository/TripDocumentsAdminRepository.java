package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.TripUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripDocumentsAdminRepository extends JpaRepository<TripUser, Long> {

    /** Nombre de types requis pour le voyage. */
    @Query(value = """
        SELECT COUNT(*)
        FROM trip_formality tf
        WHERE tf.trip_id = :tripId
          AND tf.required = TRUE
        """, nativeQuery = true)
    int countRequiredForTrip(@Param("tripId") Long tripId);

    /** Nombre de types fournis (distinct) par user parmi les types requis du voyage. */
    interface ProvidedCountRow {
        Long getUserId();
        Integer getProvidedCount();
    }

    @Query(value = """
        SELECT d.user_id AS userId,
               COUNT(DISTINCT d.document_type_id) AS providedCount
        FROM documents d
        WHERE d.user_id IN (:userIds)
          AND d.document_type_id IN (
              SELECT tf.document_type_id
              FROM trip_formality tf
              WHERE tf.trip_id = :tripId
                AND tf.required = TRUE
          )
          -- adapte selon ta sémantique métier :
          AND COALESCE(d.document_status, '') <> 'REJECTED'
        GROUP BY d.user_id
        """, nativeQuery = true)
    List<ProvidedCountRow> countProvidedByUserForTrip(@Param("tripId") Long tripId,
                                                      @Param("userIds") List<Long> userIds);
}