package fr.siovision.voyages.infrastructure.dto.authentication;

public record RegisterFinishRequest(
    String email,
    String displayName,
    String registrationRequest
) {
}
