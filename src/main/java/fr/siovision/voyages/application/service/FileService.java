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
        String safeName = originalFilename == null ? "file" : originalFilename.replaceAll("\\s+", "_");
        return "cover/" + UUID.randomUUID() + "-" + safeName;
    }

    /** URL pré-signée pour upload (PUT) direct depuis le front. */
    public Map<String, String> presignPut(String key, String contentType) {
        PutObjectRequest obj = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                // .acl(ObjectCannedACL.PUBLIC_READ) // seulement si tu exposes en public (souvent inutile)
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignMinutes))
                .putObjectRequest(obj)
                .build();

        URL url = presigner.presignPutObject(presign).url();
        return Map.of("url", url.toString(), "key", key);
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
}
