package fr.siovision.voyages.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import fr.siovision.voyages.domain.model.ParentChild;
import java.util.List;
import java.util.UUID;

public interface ParentChildRepository extends JpaRepository<ParentChild, UUID> {
    boolean existsByParentIdAndChildId(Long id, Long id1);

    List<ParentChild> findByParentId(Long parentId);

    List<ParentChild> findByChildId(Long childId);
}
