package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FirstLoginService {

    private final UserRepository userRepo;
    private final KeycloakService keycloakService;

    @Transactional
    public void handleFirstLogin(String email, String reqId) throws NoSuchAlgorithmException {
        Logger log = LoggerFactory.getLogger(FirstLoginService.class);
        String reqIdShort = reqId != null && reqId.length() >= 8 ? reqId.substring(0, 8) : "no-reqid";
        String emailHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8))
        );
        log.info("First login request [{}] for email hash {}", reqIdShort, emailHash);

        var user = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return; // réponse générique côté API
        }

        // Crée dans KC si pas encore lié
        if (user.getKeycloakId() == null) {
            String kcId = keycloakService.createUserIfAbsent(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole(),
                    true
            );
            user.setKeycloakId(kcId);
            userRepo.save(user);
        }

        // Envoie l’email (toujours, ça réinvite au besoin)
        keycloakService.sendExecuteActionsEmail(user.getKeycloakId(),
                List.of("VERIFY_EMAIL", "UPDATE_PASSWORD").toArray(new String[0]));
    }
}
