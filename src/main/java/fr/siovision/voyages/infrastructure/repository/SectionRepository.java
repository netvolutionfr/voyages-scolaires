package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface SectionRepository extends JpaRepository<Section, Long> {
    @Query("""
                SELECT s
                FROM Section s
                WHERE COALESCE(:q, '') = ''
                   OR lower(s.label) LIKE lower(concat('%', :q, '%'))
            """)
    Page<Section> search(@Param("q") String q, Pageable pageable);

    Optional<Section> findByLabel(String label);

    Optional<Section> findByPublicId(UUID secId);

    @Query("""
  SELECT s FROM Section s
  WHERE (:qLike IS NULL OR
         LOWER(s.label)       LIKE :qLike OR
         LOWER(s.description) LIKE :qLike)
    AND (:cycle IS NULL OR s.cycle = :cycle)
    AND (:year  IS NULL OR s.year  = :year)
    AND (:activeOnly = false OR s.isActive = true)
  ORDER BY
     CASE WHEN :useYearLabel = true THEN
       CASE s.year
           WHEN 'TROISIEME' THEN 1
           WHEN 'SECONDE'   THEN 2
           WHEN 'PREMIERE'  THEN 3
           WHEN 'TERMINALE' THEN 4
           WHEN 'BTS1'      THEN 5
           WHEN 'BTS2'      THEN 6
           WHEN 'B3'        THEN 7
           WHEN 'B4'        THEN 8
           WHEN 'B5'        THEN 9
           ELSE 999
       END
     ELSE NULL END,
       CASE s.cycle
              WHEN 'COLLEGE'      THEN 1
              WHEN 'LYCEE_GENERAL' THEN 2
              WHEN 'LYCEE_TECHNO'  THEN 3
              WHEN 'LYCEE_PRO'     THEN 4
              WHEN 'BTS'          THEN 5
              WHEN 'ALTERNANCE'   THEN 6
              WHEN 'PREPA'        THEN 7
              ELSE 999
         END,
     s.label ASC
  """)
    Page<Section> searchSections(
            @Param("qLike") String qLike,
            @Param("cycle") String cycle,
            @Param("year") String year,
            @Param("activeOnly") boolean activeOnly,
            @Param("useYearLabel") boolean useYearLabel,
            Pageable pageable
    );
}
