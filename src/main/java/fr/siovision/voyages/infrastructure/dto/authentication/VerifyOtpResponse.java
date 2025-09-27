package fr.siovision.voyages.infrastructure.dto.authentication;

public record VerifyOtpResponse(
        boolean active,
        String jwt
) {
}
