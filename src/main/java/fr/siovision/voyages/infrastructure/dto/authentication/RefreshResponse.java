package fr.siovision.voyages.infrastructure.dto.authentication;

import lombok.Builder;

@Builder
public record RefreshResponse(
        String token_type,          // "Bearer"
        String access_token,
        long   expires_in,          // durée du nouvel access token
        String refresh_token,       // le nouveau refresh rotatif
        long   refresh_expires_in   // durée de vie du refresh (ex: 2592000 = 30 jours)
) {}