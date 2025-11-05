package fr.siovision.voyages.application.security;

import fr.siovision.voyages.application.service.CurrentUserService;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("tripSecurity")
@RequiredArgsConstructor
@Slf4j
public class TripSecurity {

    private final CurrentUserService currentUserService;
    private final SecurityQueriesRepository q;
    private final EntityManager em;
    private final UserRepository userRepo;

    /** ADMIN → toujours OK ; TEACHER → doit être accompagnateur sur le voyage. */
    public boolean canViewTrip(Long tripId) {
        if (tripId == null) return false;
        User current = currentUserService.getCurrentUser();

        if (isAdmin(current)) return true;
        if (isTeacher(current)) {
            return q.teacherAssignedToTrip(current.getId(), tripId);
        }
        return false;
    }

    /** ADMIN → true ; TEACHER → true si le voyage de l’inscription lui appartient. */
    public boolean canViewRegistration(Long registrationId) {
        if (registrationId == null) return false;
        User current = currentUserService.getCurrentUser();

        if (isAdmin(current)) return true;
        if (!isTeacher(current)) return false;

        Long tripId = q.tripIdFromRegistration(registrationId);
        return tripId != null && q.teacherAssignedToTrip(current.getId(), tripId);
    }

    /** ADMIN → OK ; TEACHER → OK si (1) accompagnateur du voyage et (2) élève inscrit à ce voyage. */
    public boolean canViewUserDocumentsForTrip(UUID userPublicId, Long tripId) {
        Long userId = userRepo.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur inconnu"))
                .getId();

        if (userId == null || tripId == null) return false;
        User current = currentUserService.getCurrentUser();

        if (isAdmin(current)) return true;
        if (!isTeacher(current)) return false;

        return q.teacherAssignedToTrip(current.getId(), tripId)
                && q.studentInTrip(userId, tripId);
    }

    /** ADMIN → OK ; TEACHER → OK si prof accompagnateur et le document correspond à un élève de son voyage. */
    public boolean canPreviewDocument(String docPublicId) {
        if (docPublicId == null || docPublicId.isBlank()) return false;
        User current = currentUserService.getCurrentUser();

        if (isAdmin(current)) return true;
        if (!isTeacher(current)) return false;

        Long teacherId = current.getId();
        Long ownerUserId = q.userIdFromDoc(docPublicId);
        if (ownerUserId == null) return false;

        Long docTypeId = q.docTypeIdFromDoc(docPublicId);
        if (docTypeId == null) return false;

        return existsTripLinkingTeacherStudentAndDocType(teacherId, ownerUserId, docTypeId);
    }

    /** ADMIN → OK ; TEACHER → OK si prof accompagnateur et l’élève est dans le voyage. */
    public boolean canViewHealthForm(UUID userPublicId, Long tripId) {
        User current = currentUserService.getCurrentUser();

        if (isAdmin(current)) return true;
        if (!isTeacher(current)) return false;

        Long userId = userRepo.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur inconnu"))
                .getId();

        return q.teacherAssignedToTrip(current.getId(), tripId)
                && q.studentInTrip(userId, tripId);
    }

    /* Vérifie s’il existe un voyage commun entre prof, élève et type de doc */
    private boolean existsTripLinkingTeacherStudentAndDocType(Long teacherId, Long studentId, Long docTypeId) {
        String sql = """
            SELECT EXISTS(
              SELECT 1
              FROM trip_user tu_teacher
              JOIN trip_user tu_student ON tu_student.trip_id = tu_teacher.trip_id
              JOIN trip_formality tf    ON tf.trip_id = tu_teacher.trip_id
              WHERE tu_teacher.user_id = :teacherId
                AND tu_teacher.chaperone = TRUE
                AND tu_student.user_id = :studentId
                AND tf.document_type_id = :docTypeId
            )
            """;
        Boolean ok = (Boolean) em.createNativeQuery(sql)
                .setParameter("teacherId", teacherId)
                .setParameter("studentId", studentId)
                .setParameter("docTypeId", docTypeId)
                .getSingleResult();
        return Boolean.TRUE.equals(ok);
    }

    /* Helpers rôle */
    private boolean isAdmin(User u)   { return "ADMIN".equalsIgnoreCase(u.getRole().toString()); }
    private boolean isTeacher(User u) { return "TEACHER".equalsIgnoreCase(u.getRole().toString()); }
}