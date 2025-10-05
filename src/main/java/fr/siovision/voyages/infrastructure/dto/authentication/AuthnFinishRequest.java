package fr.siovision.voyages.infrastructure.dto.authentication;

import java.util.Map;

public record AuthnFinishRequest(
        String id,
        String rawId,
        String type,
        Response response,
        Map<String,Object> clientExtensionResults
){
    public record Response(
            String clientDataJSON,
            String authenticatorData,
            String signature,
            String userHandle
    ) {}
}