package fr.siovision.voyages.application.service;

import fr.siovision.voyages.infrastructure.dto.ParticipantRequest;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
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

        Response response = keycloak.realm(realm).users().create(user);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Erreur lors de la création utilisateur Keycloak : " + response.getStatus());
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // Attribution du rôle "user"
        RoleRepresentation userRole = keycloak.realm(realm).roles().get("user").toRepresentation();
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(List.of(userRole));

        // Envoi des actions de vérification d'email et de mise à jour du mot de passe
        keycloak.realm("voyages")
                .users()
                .get(userId)
                .executeActionsEmail(Arrays.asList("VERIFY_EMAIL", "UPDATE_PASSWORD"));

        return keycloak.realm(realm).users().get(userId).toRepresentation();
    }
}

