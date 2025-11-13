package fr.siovision.voyages.infrastructure.repository;

import fr.siovision.voyages.domain.model.Document;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface DocumentsAdminRepository extends JpaRepository<Document, Long> {

    /** Formalités requises pour un voyage (une ligne par type). */
    interface FormalityRow {
        Long getDocumentTypeId();
        String getAbr();
        String getLabel();
        Boolean getRequired();
        String getTripCondition();
    }

    /** Dernier dépôt de l'utilisateur pour un type donné (peut être null si rien déposé). */
    interface ProvidedRow {
        Long getDocumentTypeId();
        UUID getPublicId();       // documents.public_id
        Long getSize();
        String getMime();
        Boolean getPreviewable(); // CASE ... (mime)
        LocalDateTime getProvidedAt(); // créé ou mis à jour
    }

    /** Types requis par le voyage. */
    @Query(value = """
        SELECT
            dt.id        AS documentTypeId,
            dt.abr       AS abr,
            dt.label     AS label,
            tf.required  AS required,
            tf.trip_condition AS tripCondition
        FROM trip_formality tf
        JOIN document_type dt ON dt.id = tf.document_type_id
        WHERE tf.trip_id = :tripId
        ORDER BY dt.label
        """, nativeQuery = true)
    List<FormalityRow> listFormalitiesForTrip(@Param("tripId") Long tripId);

    /**
     * Dernier document par type requis (distinct on + updated_at/created_at).
     * NB: pas de scope par trip dans documents -> on sélectionne le plus récent global.
     */
    @Query(value = """
        WITH types AS (
            SELECT dt.id AS document_type_id
            FROM trip_formality tf
            JOIN document_type dt ON dt.id = tf.document_type_id
            WHERE tf.trip_id = :tripId
        )
        SELECT DISTINCT ON (ud.document_type_id)
               ud.document_type_id     AS documentTypeId,
               ud.public_id            AS publicId,
               ud.size                 AS size,
               ud.mime                 AS mime,
               (CASE
                   WHEN ud.mime ILIKE 'image/%' OR ud.mime = 'application/pdf' THEN TRUE
                   ELSE FALSE
                END)                   AS previewable,
               COALESCE(ud.updated_at, ud.created_at) AS providedAt,
               ud.object_key           AS objectKey
        FROM documents ud
        WHERE ud.user_id = :userId
          AND ud.document_type_id IN (SELECT document_type_id FROM types)
        ORDER BY ud.document_type_id, COALESCE(ud.updated_at, ud.created_at) DESC
        """, nativeQuery = true)
    List<ProvidedRow> findLatestProvidedPerType(@Param("userId") Long userId,
                                                @Param("tripId") Long tripId);

    /** Vérif existence d’un doc via son public_id (UUID), et récupération de l’object_key. */
    @Query(value = """
        SELECT ud.object_key
        FROM documents ud
        WHERE ud.public_id = :publicId
        LIMIT 1
        """, nativeQuery = true)
    Optional<String> findObjectKeyByPublicId(@Param("publicId") UUID publicId);
}