package fr.siovision.voyages.infrastructure.dto.authentication;

public record ChallengeResponse(
        String uuid,
        String challenge,
        String rpId,
        String rpName,
        Long timeoutMs
) {
}
