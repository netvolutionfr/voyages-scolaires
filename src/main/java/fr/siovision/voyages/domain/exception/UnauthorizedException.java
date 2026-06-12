package fr.siovision.voyages.domain.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("invalid_token");
    }
}
