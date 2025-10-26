package fr.siovision.voyages.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cust_seq")
    @SequenceGenerator(name = "cust_seq", sequenceName = "cust_seq")
    private Long id;

    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus;

    private String fileNumber;
    // dateEmission;
    private LocalDate deliveryDate;
    private LocalDate expirationDate;

    @ManyToOne
    @JoinColumn(nullable = false)
    private TripUser tripUser;

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
