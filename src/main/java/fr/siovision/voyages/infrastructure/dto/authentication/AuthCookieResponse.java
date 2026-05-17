package fr.siovision.voyages.infrastructure.dto.authentication;

public record AuthCookieResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        AuthResponse.UserInfo user
) {}
