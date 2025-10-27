package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.StudentHealthForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentHealthFormRepository extends JpaRepository<StudentHealthForm, UUID> {
    StudentHealthForm findByStudentId(Long id);
}
