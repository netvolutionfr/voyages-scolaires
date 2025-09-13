package fr.siovision.voyages.infrastructure.dto;

public record FirstLoginResponse(
        String message
) {
    public static FirstLoginResponse generic() {
        return new FirstLoginResponse(
                "Si votre adresse figure dans notre base, vous recevrez un email avec les instructions."
        );
    }
}
