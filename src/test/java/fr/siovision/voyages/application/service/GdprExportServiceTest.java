package fr.siovision.voyages.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.siovision.voyages.domain.model.*;
import fr.siovision.voyages.infrastructure.dto.gdpr.GdprExportResponse;
import fr.siovision.voyages.infrastructure.repository.DocumentRepository;
import fr.siovision.voyages.infrastructure.repository.ParentChildRepository;
import fr.siovision.voyages.infrastructure.repository.RefreshTokenRepository;
import fr.siovision.voyages.infrastructure.repository.StudentHealthFormRepository;
import fr.siovision.voyages.infrastructure.repository.WebAuthnCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GdprExportServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock DocumentRepository documentRepository;
    @Mock StudentHealthFormRepository studentHealthFormRepository;
    @Mock ParentChildRepository parentChildRepository;
    @Mock WebAuthnCredentialRepository webAuthnCredentialRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;

    private ObjectMapper objectMapper;
    private GdprExportService service;

    private User user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        HealthFormPayloadRenderer renderer = new HealthFormPayloadRenderer(objectMapper);
        service = new GdprExportService(
                currentUserService,
                documentRepository,
                studentHealthFormRepository,
                renderer,
                parentChildRepository,
                webAuthnCredentialRepository,
                refreshTokenRepository,
                objectMapper
        );

        user = new User();
        user.setId(42L);
        user.setPublicId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setEmail("eleve@example.com");
        user.setFirstName("Alice");
        user.setLastName("Dupont");
        user.setDisplayName("Alice Dupont");
        user.setGender("F");
        user.setTelephone("0601020304");
        user.setBirthDate(LocalDate.of(2010, 5, 1));
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setConsentGivenAt(LocalDate.of(2026, 1, 1));
        user.setConsentText("J'accepte le traitement de mes données.");
        user.setTrips(List.of());
        user.setTripPreferences(Set.of());
        user.setCreatedAt(LocalDateTime.of(2025, 9, 1, 8, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));

        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);
        lenient().when(documentRepository.findAllByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of());
        lenient().when(studentHealthFormRepository.findByStudentId(42L)).thenReturn(null);
        lenient().when(parentChildRepository.findByParentId(42L)).thenReturn(List.of());
        lenient().when(parentChildRepository.findByChildId(42L)).thenReturn(List.of());
        lenient().when(webAuthnCredentialRepository.findAllByUserId(42L)).thenReturn(List.of());
        lenient().when(refreshTokenRepository.findMaxLastUsedAtByUser(user)).thenReturn(Optional.empty());
    }

    @Test
    void export_mapsProfileFieldsFromUser() {
        GdprExportResponse export = service.export();

        assertThat(export.format()).isEqualTo("voyages-gdpr-export/v1");
        assertThat(export.profile().publicId()).isEqualTo(user.getPublicId());
        assertThat(export.profile().email()).isEqualTo("eleve@example.com");
        assertThat(export.profile().firstName()).isEqualTo("Alice");
        assertThat(export.profile().lastName()).isEqualTo("Dupont");
        assertThat(export.profile().gender()).isEqualTo("F");
        assertThat(export.profile().telephone()).isEqualTo("0601020304");
        assertThat(export.profile().birthDate()).isEqualTo("2010-05-01");
        assertThat(export.profile().role()).isEqualTo("STUDENT");
        assertThat(export.consent().text()).isEqualTo("J'accepte le traitement de mes données.");
    }

    @Test
    void export_withoutLegalGuardian_returnsNull() {
        GdprExportResponse export = service.export();

        assertThat(export.legalGuardian()).isNull();
    }

    @Test
    void export_withLegalGuardian_mapsGuardianIdentity() {
        User guardian = new User();
        guardian.setPublicId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        guardian.setFirstName("Bernard");
        guardian.setLastName("Dupont");
        guardian.setEmail("parent@example.com");
        user.setLegalGuardian(guardian);

        GdprExportResponse export = service.export();

        assertThat(export.legalGuardian().publicId()).isEqualTo(guardian.getPublicId());
        assertThat(export.legalGuardian().fullName()).isEqualTo("Bernard Dupont");
        assertThat(export.legalGuardian().email()).isEqualTo("parent@example.com");
    }

    @Test
    void export_familyLinks_includesBothParentAndChildRelations() {
        User child = new User();
        child.setPublicId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        child.setFirstName("Enfant");
        child.setLastName("Un");
        child.setEmail("enfant@example.com");
        ParentChild asParent = new ParentChild();
        asParent.setParent(user);
        asParent.setChild(child);

        User parent = new User();
        parent.setPublicId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        parent.setFirstName("Parent");
        parent.setLastName("Un");
        parent.setEmail("papa@example.com");
        ParentChild asChild = new ParentChild();
        asChild.setParent(parent);
        asChild.setChild(user);

        when(parentChildRepository.findByParentId(42L)).thenReturn(List.of(asParent));
        when(parentChildRepository.findByChildId(42L)).thenReturn(List.of(asChild));

        GdprExportResponse export = service.export();

        assertThat(export.familyLinks()).hasSize(2);
        assertThat(export.familyLinks())
                .anySatisfy(link -> {
                    assertThat(link.relation()).isEqualTo("CHILD");
                    assertThat(link.publicId()).isEqualTo(child.getPublicId());
                })
                .anySatisfy(link -> {
                    assertThat(link.relation()).isEqualTo("PARENT");
                    assertThat(link.publicId()).isEqualTo(parent.getPublicId());
                });
    }

    @Test
    void export_tripRegistration_excludesAdminNotesButKeepsDecisionMessage() {
        Trip trip = new Trip();
        trip.setTitle("San Francisco");
        trip.setDestination("USA");
        trip.setDepartureDate(LocalDate.of(2026, 4, 8));
        trip.setReturnDate(LocalDate.of(2026, 4, 15));

        TripUser registration = new TripUser();
        registration.setTrip(trip);
        registration.setUser(user);
        registration.setRegistrationStatus(TripRegistrationStatus.VALIDATED);
        registration.setRegistrationDate(LocalDateTime.of(2026, 1, 5, 10, 0));
        registration.setDecisionMessage("Bienvenue à bord");
        registration.setAdminNotes("Note interne confidentielle réservée à l'administration");

        user.setTrips(List.of(registration));

        GdprExportResponse export = service.export();

        assertThat(export.trips().registrations()).hasSize(1);
        var mapped = export.trips().registrations().get(0);
        assertThat(mapped.tripTitle()).isEqualTo("San Francisco");
        assertThat(mapped.decisionMessage()).isEqualTo("Bienvenue à bord");
        // GdprTripRegistrationDTO n'a structurellement pas de champ adminNotes :
        // aucune donnée interne à l'établissement ne peut fuiter dans l'export.
        assertThat(GdprExportResponseFieldNames.tripRegistrationFieldNames())
                .doesNotContain("adminNotes");
    }

    @Test
    void export_healthForm_parsesDecryptedPayloadAsJson() {
        StudentHealthForm form = new StudentHealthForm();
        form.setStudent(user);
        form.setSignedAt(Instant.parse("2026-01-10T00:00:00Z"));
        form.setValidUntil(Instant.parse("2026-12-31T00:00:00Z"));
        form.setUpdatedAt(Instant.parse("2026-01-11T00:00:00Z"));
        form.setPayload("{\"allergies\":\"aucune\"}");

        when(studentHealthFormRepository.findByStudentId(42L)).thenReturn(form);

        GdprExportResponse export = service.export();

        assertThat(export.healthForm()).isNotNull();
        assertThat(export.healthForm().payload().get("allergies").asText()).isEqualTo("aucune");
        assertThat(export.healthForm().payload().get("updatedAt").asText())
                .isEqualTo("2026-01-11T00:00:00Z");
    }

    @Test
    void export_withoutHealthForm_returnsNull() {
        GdprExportResponse export = service.export();

        assertThat(export.healthForm()).isNull();
    }

    @Test
    void export_document_excludesEncryptionMaterial() {
        DocumentType type = new DocumentType();
        type.setLabel("Passeport");

        Document document = new Document();
        document.setDocumentType(type);
        document.setOriginalFilename("passeport.pdf");
        document.setMime("application/pdf");
        document.setSize(12345L);
        document.setSha256("abc123");
        document.setFileNumber("X1234567");
        document.setDekWrapped("should-never-be-exported");
        document.setIv("should-never-be-exported");
        document.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        when(documentRepository.findAllByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(document));

        GdprExportResponse export = service.export();

        assertThat(export.documents()).hasSize(1);
        var mapped = export.documents().get(0);
        assertThat(mapped.type()).isEqualTo("Passeport");
        assertThat(mapped.originalFilename()).isEqualTo("passeport.pdf");
        assertThat(mapped.fileNumber()).isEqualTo("X1234567");
        // GdprDocumentDTO n'a structurellement pas de champ dekWrapped/iv : la DEK ne peut pas fuiter.
        assertThat(GdprExportResponseFieldNames.documentFieldNames())
                .doesNotContain("dekWrapped", "iv", "dekIv", "objectKey");
    }

    @Test
    void export_security_excludesCredentialSecretsButKeepsAaguidAndCreatedAt() {
        WebAuthnCredential credential = new WebAuthnCredential();
        credential.setUser(user);
        credential.setAaguid("00000000-0000-0000-0000-000000000000");
        credential.setCredentialId(new byte[]{1, 2, 3});
        credential.setCoseKey(new byte[]{4, 5, 6});
        credential.setCreatedAt(LocalDateTime.of(2025, 10, 1, 0, 0));

        when(webAuthnCredentialRepository.findAllByUserId(42L)).thenReturn(List.of(credential));
        when(refreshTokenRepository.findMaxLastUsedAtByUser(user))
                .thenReturn(Optional.of(Instant.parse("2026-06-30T12:00:00Z")));

        GdprExportResponse export = service.export();

        assertThat(export.security().passkeys()).hasSize(1);
        assertThat(export.security().passkeys().get(0).aaguid())
                .isEqualTo("00000000-0000-0000-0000-000000000000");
        assertThat(export.security().lastLoginAt()).isEqualTo(Instant.parse("2026-06-30T12:00:00Z"));
        assertThat(GdprExportResponseFieldNames.passkeyFieldNames())
                .doesNotContain("credentialId", "coseKey", "userHandle");
    }

    @Test
    void export_isReadOnlyTransaction() throws NoSuchMethodException {
        Method method = GdprExportService.class.getMethod("export");
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
    }

    /** Introspection utilitaire : liste les noms de composants des records DTO RGPD. */
    private static final class GdprExportResponseFieldNames {
        static List<String> tripRegistrationFieldNames() {
            return fieldNames(fr.siovision.voyages.infrastructure.dto.gdpr.GdprTripRegistrationDTO.class);
        }

        static List<String> documentFieldNames() {
            return fieldNames(fr.siovision.voyages.infrastructure.dto.gdpr.GdprDocumentDTO.class);
        }

        static List<String> passkeyFieldNames() {
            return fieldNames(fr.siovision.voyages.infrastructure.dto.gdpr.GdprPasskeyDTO.class);
        }

        private static List<String> fieldNames(Class<?> recordClass) {
            return java.util.Arrays.stream(recordClass.getRecordComponents())
                    .map(c -> c.getName())
                    .toList();
        }
    }
}
