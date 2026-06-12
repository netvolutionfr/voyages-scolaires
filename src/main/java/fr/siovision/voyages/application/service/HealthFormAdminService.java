package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.StudentHealthForm;
import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.infrastructure.dto.HealthFormAdminDTO;
import fr.siovision.voyages.infrastructure.repository.HealthFormAdminRepository;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthFormAdminService {

    private final HealthFormAdminRepository repo;
    private final UserRepository userRepo;
    private final CurrentUserService currentUserService;
    private final StudentHealthFormRepository studentHealthFormRepository;
    private final HealthFormPayloadRenderer healthFormPayloadRenderer;

    /**
     * Retourne la dernière fiche sanitaire de l'élève (si elle existe).
     * @param userPublicId élève concerné
     * @param tripId voyage invoqué (utile pour tracer la finalité de la lecture)
     */
    @Transactional(readOnly = true)
    public HealthFormAdminDTO get(UUID userPublicId, Long tripId) {

        User currentUser = currentUserService.getCurrentUser();
        User student = userRepo.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Student inconnu"));
        StudentHealthForm form = studentHealthFormRepository.findByStudentId(student.getId());

        // RGPD: journaliser l'accès + finalité (tripId)
        log.info("Accès à la fiche sanitaire de l'utilisateur ID {} par administrateur ou prof {} pour voyage {}", student.getEmail(), currentUser.getEmail(), tripId);

        String content = healthFormPayloadRenderer.render(form);

        if (content != null) {
            return new HealthFormAdminDTO(true, content);
        }
        return null;
    }

    /**
     * Répond rapidement si l'élève a (au moins) une fiche sanitaire.
     */
    @Transactional(readOnly = true)
    public boolean exists(UUID userPublicId, Long tripId) {
        Long userId = userRepo.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur inconnu"))
                .getId();
        // audit.logHealthFormHead(userId, tripId, exists); // optionnel
        return repo.existsByStudentIdNative(userId);
    }
}
