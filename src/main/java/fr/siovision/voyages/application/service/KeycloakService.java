package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.UserRole;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class KeycloakService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private Keycloak keycloak;

    @Value("${app.front.url}")
    private String frontUrl;

    @PostConstruct
    public void init() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master") // Realm d'authentification admin
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }

    /**
     * Crée un utilisateur dans le realm applicatif si absent, retourne son ID Keycloak.
     */
    public String createUserIfAbsent(String email, String firstName, String lastName, UserRole role, boolean enabled) {
        var users = keycloak.realm(realm).users().search(email, true);
        if (!users.isEmpty()) {
            return users.getFirst().getId();
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(enabled);
        user.setEmailVerified(false);
        // Attribue le rôle
        user.setRequiredActions(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));

        var response = keycloak.realm(realm).users().create(user);
        if (response.getStatus() != 201) {
            throw new IllegalStateException("Failed to create user in Keycloak: " + response.getStatus());
        }
        String location = response.getLocation().getPath();
        String userId = location.substring(location.lastIndexOf("/") + 1);

        RoleRepresentation roleRep = keycloak.realm(realm).roles().get(role.name().toLowerCase()).toRepresentation();
        if (roleRep != null) {
            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(List.of(roleRep));
        }

        return userId;
    }

    /**
     * Envoie l'email Keycloak pour exécuter les actions (ex: mot de passe).
     */
    public void sendExecuteActionsEmail(String s, String[] actions) {
        int lifespan = 3600; // secondes de validité du lien (1h ici)
        log.info("Envoi email d'actions Keycloak à l'utilisateur {}, url de retour {}", s, frontUrl);
        String redirectUri = frontUrl + "/apres-premier-acces";

        keycloak.realm(realm)
                .users()
                .get(s)
                .executeActionsEmail(clientId, redirectUri, lifespan, Arrays.asList(actions));
    }
}

