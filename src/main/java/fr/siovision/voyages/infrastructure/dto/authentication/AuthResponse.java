package fr.siovision.voyages.infrastructure.dto.authentication;

import lombok.Builder;

@Builder
public record AuthResponse(
        String tokenType,          // "Bearer"
        String accessToken,
        long   expiresIn,          // secondes (access)
        String refreshToken,       // peut être null selon le flux
        Long   refreshExpiresIn,   // secondes (peut être null)
        UserInfo user
) {
    @Builder
    public record UserInfo(String id, String email, String firstName, String lastName, String role, String status) {}
}