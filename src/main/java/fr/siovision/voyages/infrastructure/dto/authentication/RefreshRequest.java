package fr.siovision.voyages.infrastructure.dto.authentication;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refresh_token
) {}