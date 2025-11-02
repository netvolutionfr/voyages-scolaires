package fr.siovision.voyages.infrastructure.dto;

public record DocumentObjectDTO(
    String id,
    Long size,
    String mime,
    String sha256,
    String status,
    Boolean previewable
) {
    /*
    {
        "id": "b2f7…", "size": 342100, "mime": "application/pdf",
        "sha256": "…", "status": "READY", "previewable": true
      }
     */
}
