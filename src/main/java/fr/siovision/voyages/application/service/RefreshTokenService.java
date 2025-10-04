package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;
import java.util.UUID;

public interface RefreshTokenService {
    String generateOpaqueToken();
    byte[] sha256(String token);
    String issue(User user);
    String rotate(String presentedRefresh);
    void revoke(String presentedRefresh);
    int revokeFamily(UUID familyId);
    int purgeExpired();
}
