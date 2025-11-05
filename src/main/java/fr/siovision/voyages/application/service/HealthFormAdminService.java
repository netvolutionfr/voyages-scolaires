package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.HealthFormAdminDTO;
import fr.siovision.voyages.infrastructure.repository.HealthFormAdminRepository;
import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthFormAdminService {

    private final HealthFormAdminRepository repo;
    private final UserRepository userRepo;
    // private final AuditService audit; // si tu as un service d’audit, injecte-le

    /**
     * Retourne la dernière fiche sanitaire de l'élève (si elle existe).
     * @param userPublicId élève concerné
     * @param tripId voyage invoqué (utile pour tracer la finalité de la lecture)
     */
    @Transactional(readOnly = true)
    public HealthFormAdminDTO get(UUID userPublicId, Long tripId) {
        Long userId = userRepo.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur inconnu"))
                .getId();
        var rowOpt = repo.findLatestByStudentId(userId);
        if (rowOpt.isEmpty()) {
            // audit.logHealthFormAccess(userId, tripId, /*exists*/ false);
            return new HealthFormAdminDTO(false, null);
        }

        var row = rowOpt.get();

        // TODO RGPD: journaliser l'accès + finalité (tripId)
        // audit.logHealthFormAccess(userId, tripId, /*exists*/ true);

        // Si ta payload est JSON, tu la renvoies telle quelle (string).
        // Si besoin de minimiser (ex: aperçu), c'est ici que tu le ferais.
        return new HealthFormAdminDTO(true, row.getPayload());
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