package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;

public interface JwtService {
    String issuePending(String email, String regUuid);
    String issueFull(User user);
    String issueFull(String email); // optionnel
}
