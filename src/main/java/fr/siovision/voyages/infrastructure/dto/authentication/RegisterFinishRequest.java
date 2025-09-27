package fr.siovision.voyages.infrastructure.dto.authentication;

public record RegisterFinishRequest(
        String uuid,
        String email,
        String id,
        String clientDataJSON,
        String attestationObject,
        String transports
) {
}
