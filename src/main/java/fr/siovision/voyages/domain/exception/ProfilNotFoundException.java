package fr.siovision.voyages.domain.exception;

public class ProfilNotFoundException  extends RuntimeException {
    public ProfilNotFoundException(String email) {
        super("Profil not found with email: " + email);
    }
}
