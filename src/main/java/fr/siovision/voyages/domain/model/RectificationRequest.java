package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Demande de rectification (Art. 16 RGPD) d'un champ d'état civil, tracée pour respecter le délai de réponse (Art. 12). */
@Entity
@Table(name = "rectification_request",
        indexes = {
                @Index(name = "idx_rectification_request_user", columnList = "user_id"),
                @Index(name = "idx_rectification_request_status", columnList = "status")
        })
@Getter
@Setter
public class RectificationRequest {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false, length = 32)
    private RectificationField field;

    @Column(name = "requested_value", nullable = false, length = 255)
    private String requestedValue;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RectificationStatus status = RectificationStatus.PENDING;

    @Column(name = "admin_comment", length = 1000)
    private String adminComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
