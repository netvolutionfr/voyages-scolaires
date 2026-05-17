package fr.siovision.voyages.infrastructure.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OtpIssueRequest(
        @NotBlank @Email String email
) {
}
