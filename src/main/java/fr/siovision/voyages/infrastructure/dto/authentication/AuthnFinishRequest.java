package fr.siovision.voyages.infrastructure.dto.authentication;

//{
//        "id": "CredIdBase64Url",
//        "authenticatorData": "Base64Url",
//        "clientDataJSON": "Base64Url",
//        "signature": "Base64Url",
//        "userHandle": "Base64UrlOrNull"
//        }

public record AuthnFinishRequest(
    String id,
    String authenticatorData,
    String clientDataJSON,
    String signature,
    String userHandle
) {
}
