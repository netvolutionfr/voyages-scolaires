package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.ParentChild;
import fr.siovision.voyages.domain.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ParentChildRepository extends JpaRepository<ParentChild, UUID> {
    boolean existsByParentIdAndChildId(Long id, Long id1);

    List<ParentChild> findByParentId(Long parentId);

    List<ParentChild> findByChildId(Long childId);

    // Effacement RGPD (ADR-0006) : un tuteur d'enfant encore actif ne peut pas être effacé
    boolean existsByParent_IdAndChild_Status(Long parentId, UserStatus status);

    @Modifying
    @Query("delete from ParentChild pc where pc.parent.id = :userId or pc.child.id = :userId")
    void deleteAllByParentIdOrChildId(@Param("userId") Long userId);
}
