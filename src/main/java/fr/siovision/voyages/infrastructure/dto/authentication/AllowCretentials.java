package fr.siovision.voyages.infrastructure.dto.authentication;

import jakarta.validation.constraints.NotNull;

public class AllowCretentials {
    @NotNull
    public String id;

    @NotNull
    public String type;

    @NotNull
    public String[] transports;
}
