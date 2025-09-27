package fr.siovision.voyages.infrastructure.dto.authentication;

public record VerifyOtpRequest(
        String email,
        String otp
) {
}
