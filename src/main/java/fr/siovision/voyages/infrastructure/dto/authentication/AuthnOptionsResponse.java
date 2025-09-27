package fr.siovision.voyages.infrastructure.dto.authentication;

public record AuthnOptionsResponse(
        String challenge,
        String timeoutMs,
        String rpId,
        AllowCretentials[] allowCredentials,
        String userVerification
) {
}
