package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_documents")
public class UserDocument {
    public enum Status {PENDING_UPLOAD, UPLOADED, READY, REJECTED}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private DocumentType documentType;

    /**
     * Clé S3/MinIO (ex: org/.../formalities/{documentTypeId}/{uuid})
     */
    @Column(nullable = false, length = 512)
    private String objectKey;

    private Long size;
    private String mime;
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING_UPLOAD;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}