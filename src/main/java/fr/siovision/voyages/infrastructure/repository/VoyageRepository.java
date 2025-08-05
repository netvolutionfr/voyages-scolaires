package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Voyage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VoyageRepository extends JpaRepository<Voyage, Long> {
    @Query("SELECT v FROM Voyage v WHERE " +
            "v.dateDebutInscription <= CURRENT_DATE AND " +
            "v.dateFinInscription >= CURRENT_DATE")
    List<Voyage> findVoyagesOuverts();
}
