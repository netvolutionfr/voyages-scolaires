package fr.siovision.voyages.application.service;

import fr.siovision.voyages.domain.model.User;

public record RotateResult(
        User user,
        String newRefreshToken
) {
}
