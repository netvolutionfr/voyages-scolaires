package fr.siovision.voyages.infrastructure.dto.authentication;

import lombok.Builder;

@Builder
public record RegisterFinishResponse(
        String tokenType,          // "Bearer"
        String accessToken,
        long   expiresIn,          // secondes (access)
        RegisterFinishResponse.UserInfo user
) {
    @Builder
    public record UserInfo(String id, String email, String firstName, String lastName, String role, String status) {}
}

