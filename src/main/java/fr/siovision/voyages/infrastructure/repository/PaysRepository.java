package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.FormalitePaysTemplate;
import fr.siovision.voyages.domain.model.Pays;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaysRepository extends JpaRepository<Pays, Long> {
    @Query("""
                SELECT p
                FROM Pays p
                WHERE COALESCE(:q, '') = ''
                   OR lower(p.nom) LIKE lower(concat('%', :q, '%'))
            """)
    Page<Pays> search(@Param("q") String q, Pageable pageable);
}
