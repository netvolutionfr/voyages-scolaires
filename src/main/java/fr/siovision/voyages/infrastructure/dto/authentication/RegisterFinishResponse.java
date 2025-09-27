package fr.siovision.voyages.infrastructure.dto.authentication;

public record RegisterFinishResponse(
        boolean pending,
        String jwtPending
) {
}
