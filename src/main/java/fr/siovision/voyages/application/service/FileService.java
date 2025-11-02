package fr.siovision.voyages.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Presigner presigner;

    @Value("${app.s3.bucket}") private String bucket;
    @Value("${app.s3.presign-duration-min:10}") private int presignMinutes;

    /** Génère une clé unique pour une couverture de voyage. */
    public String buildCoverKey(String originalFilename) {
        return "cover/" + UUID.randomUUID() + "-" + sanitize(originalFilename);
    }

    /** Génère une clé canonique pour un document utilisateur. */
    public String buildDocumentKey(long userId, String docCode, String originalFilename) {
        if (docCode == null || docCode.isBlank()) docCode = "general";
        final LocalDate d = LocalDate.now();
        return String.format(
                "users/%d/%s/%04d/%02d/%s-%s",
                userId,
                docCode.toLowerCase(Locale.ROOT),
                d.getYear(),
                d.getMonthValue(),
                UUID.randomUUID(),
                sanitize(originalFilename)
        );
    }

    /** URL pré-signée pour upload (PUT) direct depuis le front. */
    public Map<String, String> presignPut(String key, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        PutObjectRequest obj = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .putObjectRequest(obj)
                .build();

        URL url = presigner.presignPutObject(presign).url();
        // compat : on renvoie "key" (existant) ET "objectKey" (pratique pour /attach)
        return Map.of("url", url.toString(), "key", key, "objectKey", key);
    }

    /** URL pré-signée de téléchargement (GET), si tu veux servir des objets privés. */
    public String presignGet(String key) {
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .getObjectRequest(req)
                .build();

        return presigner.presignGetObject(presign).url().toString();
    }

    /** Petit nettoyage de nom de fichier. */
    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        String n = name.strip()
                .replace("\\", "_").replace("/", "_")
                .replaceAll("\\s+", "-")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (n.length() > 120) {
            int dot = n.lastIndexOf('.');
            if (dot > 0 && dot < n.length() - 1) {
                String base = n.substring(0, dot);
                String ext = n.substring(dot);
                n = base.substring(0, Math.min(100, base.length())) + ext;
            } else {
                n = n.substring(0, 120);
            }
        }
        return n;
    }
}
