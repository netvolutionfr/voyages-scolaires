package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.events.DocumentStorageDeletionEvent;
import fr.siovision.voyages.domain.exception.ErasureBlockedException;
import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Droit à l'effacement (Art. 17 RGPD) : suppression totale (ADR-0003), garde-fous (ADR-0006),
 * mineurs exclus du self-service (ADR-0005 — voir {@link #eraseSelf}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserErasureService {

    private static final List<TripRegistrationStatus> ACTIVE_REGISTRATION_STATUSES = List.of(
            TripRegistrationStatus.VALIDATED, TripRegistrationStatus.ENROLLED, TripRegistrationStatus.CONFIRMED
    );

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final StudentHealthFormRepository studentHealthFormRepository;
    private final ParentChildRepository parentChildRepository;
    private final TripUserRepository tripUserRepository;
    private final TripPreferenceRepository tripPreferenceRepository;
    private final TripRepository tripRepository;
    private final RectificationRequestRepository rectificationRequestRepository;
    private final ApplicationEventPublisher events;

    /** Effacement en self-service. Un STUDENT mineur ne peut pas s'auto-effacer (ADR-0005). */
    @Transactional
    public void eraseSelf(User user) {
        if (user.getRole() == UserRole.STUDENT) {
            throw new ErasureBlockedException("student_self_erasure_forbidden", org.springframework.http.HttpStatus.FORBIDDEN);
        }
        erase(user);
    }

    /** Effacement — utilisé par le self-service et par l'admin (mêmes garde-fous dans les deux cas). */
    @Transactional
    public void erase(User user) {
        checkGuardRails(user);

        deleteDocuments(user);
        studentHealthFormRepository.deleteByStudentId(user.getId());
        removeChaperoneLinks(user);
        tripUserRepository.deleteTripUsersJoinRows(user.getId());
        tripUserRepository.deleteAllByUser(user);
        tripPreferenceRepository.deleteAllByUser(user);
        parentChildRepository.deleteAllByParentIdOrChildId(user.getId());
        rectificationRequestRepository.clearProcessedBy(user);
        rectificationRequestRepository.deleteAllByUser(user);

        UUID publicId = user.getPublicId();
        userRepository.delete(user);
        log.info("Compte utilisateur {} effacé (RGPD Art. 17)", publicId);
    }

    private void checkGuardRails(User user) {
        if (user.getRole() == UserRole.ADMIN
                && user.getStatus() == UserStatus.ACTIVE
                && userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new ErasureBlockedException("last_active_admin");
        }

        List<TripUser> activeRegistrations = tripUserRepository.findActiveByUser(
                user.getId(), ACTIVE_REGISTRATION_STATUSES, LocalDate.now());
        if (!activeRegistrations.isEmpty()) {
            throw new ErasureBlockedException("active_trip_registration");
        }

        boolean guardianOfActiveChild = userRepository.existsByLegalGuardianAndStatus(user, UserStatus.ACTIVE)
                || parentChildRepository.existsByParent_IdAndChild_Status(user.getId(), UserStatus.ACTIVE);
        if (guardianOfActiveChild) {
            throw new ErasureBlockedException("guardian_of_active_student");
        }
    }

    private void deleteDocuments(User user) {
        List<Document> documents = documentRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        documentRepository.deleteUserDocumentLinks(user.getId());
        for (Document document : documents) {
            String objectKey = document.getObjectKey();
            documentRepository.delete(document);
            events.publishEvent(new DocumentStorageDeletionEvent(objectKey));
        }
    }

    private void removeChaperoneLinks(User user) {
        Page<Trip> trips = tripRepository.findByChaperonesContaining(user, Pageable.unpaged());
        for (Trip trip : trips) {
            trip.getChaperones().remove(user);
            tripRepository.save(trip);
        }
    }
}
