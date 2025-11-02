package fr.siovision.voyages.infrastructure.dto;

public record DocumentWarningDTO(
    String code,
    String message
) {
    /*
    { "code": "EXPIRES_SOON", "message": "Expire < 6 mois après le retour" }
     */
}
