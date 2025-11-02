package fr.siovision.voyages.infrastructure.dto;

public record DocumentTypeDetailDTO(
    Long id,
    String code,
    String label,
    String kind,
    String[] acceptedMime,
    Integer maxSizeMb,
    String scope
) {
    /*
    {
        "id": 10, "code": "passport", "label": "Passeport",
        "kind": "FILE",
        "acceptedMime": ["application/pdf","image/jpeg","image/png"],
        "maxSizeMb": 10,
        "scope": "GENERAL"
      }
     */
}
