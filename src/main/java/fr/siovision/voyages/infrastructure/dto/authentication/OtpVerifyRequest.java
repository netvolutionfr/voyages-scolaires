package fr.siovision.voyages.infrastructure.dto.authentication;

public record OtpVerifyRequest(
        String email,
        String otp
) {}
