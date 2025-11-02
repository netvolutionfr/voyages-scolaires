package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
        select d
        from Document d
        where d.user.id = :userId
          and d.documentStatus = 'READY'
        order by d.documentType.id asc, d.createdAt desc
    """)
    List<Document> findAllReadyByUser(@Param("userId") Long userId);

    Optional<Document> findByPublicId(UUID publicId);

    @Query("""
        select d from Document d
        where d.publicId = :publicId and d.user.id = :userId
    """)
    Optional<Document> findOwned(@Param("publicId") UUID publicId,
                                 @Param("userId") Long userId);

    // previous latest (if any) BEFORE inserting the new one
    Optional<Document> findFirstByUserIdAndDocumentTypeIdOrderByCreatedAtDesc(Long userId, Long documentTypeId);
}