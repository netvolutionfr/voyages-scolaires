package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.StudentHealthForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentHealthFormRepository extends JpaRepository<StudentHealthForm, UUID> {
    StudentHealthForm findByStudentId(Long id);

    @Query("""
        select f from StudentHealthForm f
        where f.student.id = :userId
        order by
          case when f.signedAt is null then 1 else 0 end asc,
          f.signedAt desc nulls last,
          f.updatedAt desc
    """)
    Optional<StudentHealthForm> findLatestByUserId(@Param("userId") Long userId);

    int deleteByValidUntilBefore(Instant now);

    // Returns ALL forms for the userIds, newest first (signedAt takes precedence over updatedAt)
    @Query("""
        select f from StudentHealthForm f
        where f.student.id in :userIds
        order by f.student.id asc,
                 case when f.signedAt is null then 1 else 0 end asc,
                 f.signedAt desc nulls last,
                 f.updatedAt desc
    """)
    List<StudentHealthForm> findAllSortedByUserAndRecency(@Param("userIds") List<Long> userIds);

    @Query("""
    select f from StudentHealthForm f
    where f.student.publicId in :userPublicIds
    order by f.student.publicId asc,
             case when f.signedAt is null then 1 else 0 end asc,
             f.signedAt desc nulls last,
             f.updatedAt desc
""")
    List<StudentHealthForm> findAllSortedByUserAndRecencyByPublicIds(@Param("userPublicIds") Collection<UUID> userPublicIds);
}
