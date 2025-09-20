package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import fr.siovision.voyages.domain.model.UserRole;

public interface UserProvisioningService {
    User upsertFromKeycloak(
            String keycloakId,
            String email,
            String lastName,
            String firstName,
            UserRole preferredRole
    );
}
