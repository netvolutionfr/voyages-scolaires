package fr.siovision.voyages.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FirstLoginRequest (
    @NotBlank
    @Email
    String email
) {}
