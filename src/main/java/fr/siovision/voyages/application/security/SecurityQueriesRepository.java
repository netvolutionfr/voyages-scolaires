package fr.siovision.voyages.application.security;

import fr.siovision.voyages.domain.model.TripUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityQueriesRepository extends JpaRepository<TripUser, Long> {

    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM trip_chaperones tc
          WHERE tc.trip_id = :tripId
            AND tc.chaperones_id = :teacherId
        )
        """, nativeQuery = true)
    boolean teacherAssignedToTrip(@Param("teacherId") Long teacherId, @Param("tripId") Long tripId);

    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM trip_user tu
          WHERE tu.trip_id = :tripId
            AND tu.user_id = :studentId
        )
        """, nativeQuery = true)
    boolean studentInTrip(@Param("studentId") Long studentId, @Param("tripId") Long tripId);

    @Query(value = "SELECT tu.trip_id FROM trip_user tu WHERE tu.id = :registrationId", nativeQuery = true)
    Long tripIdFromRegistration(@Param("registrationId") Long registrationId);

    @Query(value = "SELECT ud.user_id FROM documents ud WHERE ud.public_id = CAST(:docPublicId AS UUID)", nativeQuery = true)
    Long userIdFromDoc(@Param("docPublicId") String docPublicId);

    @Query(value = "SELECT ud.document_type_id FROM documents ud WHERE ud.public_id = CAST(:docPublicId AS UUID)", nativeQuery = true)
    Long docTypeIdFromDoc(@Param("docPublicId") String docPublicId);

    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM trip_formality tf
          WHERE tf.trip_id = :tripId
            AND tf.document_type_id = :docTypeId
        )
        """, nativeQuery = true)
    boolean docTypeRequiredForTrip(@Param("tripId") Long tripId, @Param("docTypeId") Long docTypeId);
}