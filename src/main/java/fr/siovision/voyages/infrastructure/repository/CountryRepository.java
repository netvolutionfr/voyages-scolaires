package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CountryRepository extends JpaRepository<Country, Long> {
    @Query("""
                SELECT p
                FROM Country p
                WHERE COALESCE(:q, '') = ''
                   OR lower(p.name) LIKE lower(concat('%', :q, '%'))
            """)
    Page<Country> search(@Param("q") String q, Pageable pageable);
}
