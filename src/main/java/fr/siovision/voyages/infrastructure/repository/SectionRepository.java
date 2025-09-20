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
}
