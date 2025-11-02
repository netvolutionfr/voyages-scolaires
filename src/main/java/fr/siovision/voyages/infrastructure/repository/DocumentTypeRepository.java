package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.DocumentScope;
import fr.siovision.voyages.domain.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    List<DocumentType> findByScope(DocumentScope documentScope);
}