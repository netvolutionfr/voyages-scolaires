package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.TripRegistrationStatus;
import fr.siovision.voyages.domain.model.TripUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripUserRepository extends JpaRepository<TripUser, Long>, JpaSpecificationExecutor<TripUser> {

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

    Page<TripUser> findAll(Specification<TripUser> spec, Pageable pageable);

    interface TripRegistrationRow {
        Long getRegistrationId();
        LocalDateTime getRegisteredAt();
        String getStatus();
        UUID getUserPublicId();
        Long getUserId();
        String getFirstName();
        String getLastName();
        String getEmail();
        Long getSectionId();
        String getSectionLabel();
        Long getTripId();
        String getTripTitle();
    }

    @Query(
            value = """
            SELECT
                tu.id                AS registrationId,
                tu.registration_date AS registeredAt,
                tu.registration_status::text AS status,
                u.public_id          AS userPublicId,
                u.id                 AS userId,
                u.first_name         AS firstName,
                u.last_name          AS lastName,
                u.email              AS email,
                s.id                 AS sectionId,
                s.label              AS sectionLabel,
                t.id                 AS tripId,
                t.title              AS tripTitle
            FROM trip_user tu
            JOIN users u   ON u.id = tu.user_id
            LEFT JOIN section s ON s.id = u.section_id
            JOIN trip t    ON t.id = tu.trip_id
            WHERE tu.trip_id = :tripId
              AND (:statusCode IS NULL OR tu.registration_status = :statusCode)
              AND (:sectionId IS NULL OR s.id = :sectionId)
              AND (
                    :q IS NULL
                 OR  LOWER(u.first_name) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR  LOWER(u.last_name)  LIKE LOWER(CONCAT('%', :q, '%'))
                 OR  LOWER(u.email)      LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM trip_user tu
            JOIN users u   ON u.id = tu.user_id
            LEFT JOIN section s ON s.id = u.section_id
            WHERE tu.trip_id = :tripId
              AND (:statusCode IS NULL OR tu.registration_status = :statusCode)
              AND (:sectionId IS NULL OR s.id = :sectionId)
              AND (
                    :q IS NULL
                 OR  LOWER(u.first_name) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR  LOWER(u.last_name)  LIKE LOWER(CONCAT('%', :q, '%'))
                 OR  LOWER(u.email)      LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """,
            nativeQuery = true
    )
    Page<TripRegistrationRow> searchAdminRegistrations(@Param("tripId") Long tripId,
                                                       @Param("statusCode") Integer statusCode,
                                                       @Param("q") String q,
                                                       @Param("sectionId") Long sectionId,
                                                       Pageable pageable);

    @Query(
            value = """
            SELECT
                tu.id                AS registrationId,
                tu.registration_date AS registeredAt,
                tu.registration_status::text AS status,
                u.public_id          AS userPublicId,
                u.id                 AS userId,
                u.first_name         AS firstName,
                u.last_name          AS lastName,
                u.email              AS email,
                s.id                 AS sectionId,
                s.label              AS sectionLabel,
                t.id                 AS tripId,
                t.title              AS tripTitle
            FROM trip_user tu
            JOIN users u   ON u.id = tu.user_id
            LEFT JOIN section s ON s.id = u.section_id
            JOIN trip t    ON t.id = tu.trip_id
            WHERE tu.id = :registrationId
            """,
            nativeQuery = true
    )
    Optional<TripRegistrationRow> findAdminRegistrationRow(@Param("registrationId") Long registrationId);
}