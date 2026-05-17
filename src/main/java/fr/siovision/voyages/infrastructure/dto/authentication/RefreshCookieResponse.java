package fr.siovision.voyages.infrastructure.dto.authentication;

public record RefreshCookieResponse(
        String tokenType,
        String accessToken,
        long expiresIn
) {}
