package fr.siovision.voyages.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class FirstLoginService {
    private static final Logger log = LoggerFactory.getLogger(FirstLoginService.class);

    private final KeycloakService keycloak; // Ton service existant

    public FirstLoginService(KeycloakService keycloak) {
        this.keycloak = keycloak;
    }

    public void handleFirstLogin(String rawEmail, String requestId) {
        String email = normalize(rawEmail);

        // log non-énumérant (email haché)
        log.info("first-login request reqId={} emailHash={}", requestId, sha256(email.toLowerCase()));

        try {
            Optional<String> userId = keycloak.findUserByEmail(email).getId().describeConstable();
            // Déclenche l’email d’actions (vérif email + choix mdp)
            userId.ifPresent(s -> keycloak.sendExecuteActionsEmail(
                    s,
                    new String[]{"VERIFY_EMAIL", "UPDATE_PASSWORD"}
            ));
        } catch (Exception e) {
            // Ne pas propager pour éviter l’énumération ; log technique uniquement
            log.warn("first-login failed reqId={} reason={}", requestId, e.toString());
        }
        // Toujours silence côté API (réponse générique envoyée par le controller)
    }

    private String normalize(String email) {
        return email.trim();
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "hash-error";
        }
    }
}
