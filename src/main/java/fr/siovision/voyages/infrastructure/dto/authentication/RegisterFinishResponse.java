package fr.siovision.voyages.infrastructure.dto.authentication;

public record RegisterFinishResponse(
        String jwt,
        String status,
        String userFirstName,
        String userLastName,
        String userEmail
) {
}
