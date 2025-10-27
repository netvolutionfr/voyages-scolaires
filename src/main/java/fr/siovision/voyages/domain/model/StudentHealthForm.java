package fr.siovision.voyages.domain.model;

import fr.siovision.voyages.infrastructure.converter.CryptoConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_health_form",
        indexes = { @Index(name="idx_shf_student", columnList = "student_id") })
@Getter @Setter
public class StudentHealthForm {

    @Id @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student; // récupéré via le JWT dans le service

    /** JSON complet chiffré (une seule colonne) */
    @Convert(converter = CryptoConverter.class)
    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "signed_at")     private Instant signedAt;
    @Column(name = "valid_until")   private Instant validUntil;
    @CreationTimestamp              @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp                @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version                        @Column(name = "version", nullable = false) private long version;
}