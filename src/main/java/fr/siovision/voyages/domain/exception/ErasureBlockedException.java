package fr.siovision.voyages.domain.exception;

import lombok.Getter;

/** Effacement RGPD (Art. 17(3)) refusé — voir docs/adr/0006. Le code est un identifiant machine, pas un message utilisateur. */
@Getter
public class ErasureBlockedException extends RuntimeException {
    private final String code;

    public ErasureBlockedException(String code) {
        super(code);
        this.code = code;
    }
}
