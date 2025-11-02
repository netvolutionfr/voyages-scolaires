package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "documents",
        indexes = {
                @Index(name = "idx_documents_user", columnList = "user_id"),
                @Index(name = "idx_documents_type", columnList = "document_type_id"),
                @Index(name = "idx_documents_status", columnList = "documentStatus"),
                @Index(name = "idx_documents_created_at", columnList = "createdAt")
        })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    public enum DocumentStatus { PENDING_UPLOAD, UPLOADED, READY, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    /** Identifiant public pour l’API/UI (stable, non devinable) */
    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId = UUID.randomUUID();

    /** Nom d’origine (affichage) */
    private String originalFilename;

    /** MIME réel (ex: application/pdf, image/jpeg) */
    @Column(length = 128)
    private String mime;

    /** Taille en octets */
    private Long size;

    /** Empreinte binaire (base16/base64 de la SHA-256 du corps) */
    @Column(length = 88) // 44 (base64) / 64 (hex) selon ton choix
    private String sha256;

    /** Clé S3/MinIO (jamais d'URL persistante) */
    @Column(nullable = false, length = 512)
    private String objectKey;

    /** Type fonctionnel (passeport, ESTA, etc.) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private DocumentType documentType;

    /** Portage status du flux d’upload + validations */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentStatus documentStatus = DocumentStatus.PENDING_UPLOAD;

    /** Champs métiers éventuels (numéro de doc, dates, utiles plus tard pour warnings/IA) */
    @Column(length = 64)
    private String fileNumber;

    /** Enveloppe crypto (Base64) */
    @Column(length = 512)
    private String dekWrapped;  // DEK chiffrée par KEK

    @Column(length = 32)
    private String dekIv;       // IV utilisé pour wrap la DEK

    @Column(length = 32)
    private String iv;          // IV utilisé pour chiffrer le fichier

    private LocalDate deliveryDate;
    private LocalDate expirationDate;

    /** Propriétaire du doc (élève/parent) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
