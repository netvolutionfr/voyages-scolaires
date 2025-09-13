package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.ParticipantRequest;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
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
import java.util.Map;

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

    @Value("${APP.FRONT_URL:http://localhost:5173}")
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

    public UserRepresentation findUserByEmail(String email) {
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .search(email, true);
        return users.stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    public UserRepresentation createUser(ParticipantRequest dto) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getEmail());
        user.setFirstName(dto.getPrenom());
        user.setLastName(dto.getNom());
        user.setAttributes(Map.of("locale", List.of("fr")));

        String userId;
        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() != 201) {
                throw new RuntimeException("Erreur lors de la création utilisateur Keycloak : " + response.getStatus());
            }

            userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création utilisateur Keycloak", e);
        }

        // Attribution du rôle "student"
        RoleRepresentation userRole = keycloak.realm(realm).roles().get("student").toRepresentation();
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(List.of(userRole));

        // Envoi des actions de vérification d'email et de mise à jour du mot de passe
//        try {
//            keycloak.realm("voyages")
//                    .users()
//                    .get(userId)
//                    .executeActionsEmail(Arrays.asList("VERIFY_EMAIL", "UPDATE_PASSWORD"));
//        } catch (jakarta.ws.rs.WebApplicationException ex) {
//            Response r = ex.getResponse();
//            String body = r.hasEntity() ? r.readEntity(String.class) : "";
//            log.error("KC executeActionsEmail failed: status={} body={}", r.getStatus(), body, ex);
//            throw ex;
//        } catch (jakarta.ws.rs.ProcessingException ex) {
//            log.error("KC client processing error", ex);
//            throw ex;
//        }

        return keycloak.realm(realm).users().get(userId).toRepresentation();
    }

    public void sendExecuteActionsEmail(String s, String[] actions) {
        int lifespan = 3600; // secondes de validité du lien (1h ici)
        String redirectUri = frontUrl + "/apres-premier-acces";

        keycloak.realm(realm)
                .users()
                .get(s)
                .executeActionsEmail(clientId, redirectUri, lifespan, Arrays.asList(actions));
    }
}

