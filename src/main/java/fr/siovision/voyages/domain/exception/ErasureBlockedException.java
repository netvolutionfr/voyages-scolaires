package fr.siovision.voyages.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Effacement RGPD (Art. 17(3)) refusé — voir docs/adr/0006. Le code est un identifiant machine, pas un message utilisateur. */
@Getter
public class ErasureBlockedException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    /** Garde-fous d'état (dernier admin, voyage actif, tuteur actif) — conflit avec l'état courant. */
    public ErasureBlockedException(String code) {
        this(code, HttpStatus.CONFLICT);
    }

    /** Interdiction d'agir (ex. mineur qui tente de s'auto-effacer, ADR-0005) — pas un conflit d'état. */
    public ErasureBlockedException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
    }
}
