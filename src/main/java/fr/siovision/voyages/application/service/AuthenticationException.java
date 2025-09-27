package fr.siovision.voyages.application.service;

public class AuthenticationException extends RuntimeException {
    private final int status;
    public AuthenticationException(int status, String message){ super(message); this.status=status; }
    public int status(){ return status; }
    public static AuthenticationException bad(String m){ return new AuthenticationException(400,m); }
    public static AuthenticationException forbidden(String m){ return new AuthenticationException(403,m); }
    public static AuthenticationException conflict(String m){ return new AuthenticationException(409,m); }
    public static AuthenticationException unauthorized(String m){ return new AuthenticationException(401,m); }
}
