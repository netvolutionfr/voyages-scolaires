package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "otp_tokens", indexes = {
        @Index(name = "idx_otp_user_status", columnList = "user_id,status"),
        @Index(name = "idx_otp_expires_at", columnList = "expiresAt")
})
public class OtpToken {

    public enum Purpose { ACCOUNT_VERIFICATION }
    public enum Status { PENDING, USED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "otp_seq")
    @SequenceGenerator(name = "otp_seq", sequenceName = "otp_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Purpose purpose;

    @Column(nullable = false)
    private String codeHash; // BCrypt

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private int attempts; // nb de tentatives de vérif

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant consumedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = Status.PENDING;
        if (attempts < 0) attempts = 0;
    }
}