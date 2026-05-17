package fr.siovision.voyages.infrastructure.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{4,8}", message = "OTP must be 4–8 digits") String otp
) {}
