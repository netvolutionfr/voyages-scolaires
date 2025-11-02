package fr.siovision.voyages.web;

import fr.siovision.voyages.application.security.EncryptionService;
import fr.siovision.voyages.application.service.CurrentUserService;
import fr.siovision.voyages.application.service.StorageService;
import fr.siovision.voyages.domain.model.UserRole;
import fr.siovision.voyages.infrastructure.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/documents")
@RequiredArgsConstructor
public class DocumentPreviewStreamController {
    private final CurrentUserService currentUserService;
    private final StorageService storageService;
    private final EncryptionService encryptionService;
    private final DocumentRepository documentRepository;

    @GetMapping(value = "/{docId}/preview")
    public ResponseEntity<StreamingResponseBody> preview(@PathVariable UUID docId) {
        // 1) Charge & contrôle d’accès
        var user = currentUserService.getCurrentUser();

        var doc = documentRepository.findByPublicId(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable"));

        // Autorisations minimales (adapte si besoin: parent/teacher/chaperone/admin)
        boolean owner = doc.getUser() != null && doc.getUser().getId().equals(user.getId());
        boolean isAdminOrTeacher = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.TEACHER;
        if (!owner && !isAdminOrTeacher) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }

        // 2) Prépare le déchiffrement
        if (doc.getIv() == null || doc.getDekWrapped() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Métadonnées de chiffrement manquantes");
        }


        final byte[] iv = EncryptionService.b64d(doc.getIv());
        final byte[] wrappedDek = EncryptionService.b64d(doc.getDekWrapped());

        final SecretKey dek;
        try {
            dek = encryptionService.unwrapDek(wrappedDek); // ← plus de dekIv ici
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Impossible de dérouler la DEK", e);
        }

        // 3) Ouvre l’objet S3 (ciphertext) et crée le flux déchiffrant
        final ResponseInputStream<GetObjectResponse> s3Stream;
        try {
            s3Stream = storageService.getObject(doc.getObjectKey());
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lecture stockage impossible", e);
        }

        // 4) Construit le StreamingResponseBody (pipe S3 cipher → AES/GCM → response out)
        StreamingResponseBody body = out -> {
            try (s3Stream) {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, dek, new GCMParameterSpec(128, iv));

                try (CipherInputStream cis = new CipherInputStream(s3Stream, cipher)) {
                    byte[] buffer = new byte[8192];
                    int r;
                    while ((r = cis.read(buffer)) != -1) {
                        out.write(buffer, 0, r);
                    }
                    out.flush();
                }
            } catch (IOException ioe) {
                // I/O côté client (fermeture onglet) → log soft
                // (laisse Spring gérer la connexion interrompue)
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de déchiffrement", e);
            }
        };

        // 5) En-têtes HTTP (inline + pas de cache)
        String mime = Optional.ofNullable(doc.getMime()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String filename = Optional.ofNullable(doc.getOriginalFilename()).orElse("document");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename.replace("\"","") + "\"")
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(body);
    }
}