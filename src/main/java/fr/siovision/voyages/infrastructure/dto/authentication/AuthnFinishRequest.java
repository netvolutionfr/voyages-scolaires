package fr.siovision.voyages.infrastructure.dto.authentication;

//{
//        "id": "CredIdBase64Url",
//        "authenticatorData": "Base64Url",
//        "clientDataJSON": "Base64Url",
//        "signature": "Base64Url",
//        "userHandle": "Base64UrlOrNull"
//        }

import jakarta.validation.constraints.NotNull;

public record AuthnFinishRequest(
        @NotNull
        String id,

        @NotNull
        String authenticatorData,

        @NotNull
        String clientDataJSON,

        @NotNull
        String signature,

        @NotNull
        String userHandle
) {
}
