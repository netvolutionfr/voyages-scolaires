package fr.siovision.voyages.application.service;

import fr.siovision.voyages.application.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3;
    private final EncryptionService encryption;
    @Value("${app.s3.bucket}") private String bucket;

    public record EncryptedUploadResult(
            String objectKey,
            String ivB64,
            String dekWrappedB64,
            long ciphertextSize
    ) {}

    public String buildEncryptedKey(String originalFilename) {
        String safe = (originalFilename == null ? "file" : originalFilename).replaceAll("\\s+", "_");
        return "docs/" + java.util.UUID.randomUUID() + "-" + safe;
    }

    public EncryptedUploadResult putEncrypted(String objectKey, InputStream plaintext, String origMime) throws Exception {
        // lis en mémoire (OK si ≤ ~15 Mo)
        byte[] plain = plaintext.readAllBytes();

        // DEK + IV
        SecretKey dek = encryption.generateDek();
        byte[] iv = encryption.newIv();

        // chiffre
        byte[] cipherText = encryption.encryptAesGcm(plain, dek, iv);

        // wrap DEK (AESWrap) — pas de dekIv
        byte[] wrapped = encryption.wrapDek(dek);

        // push S3 (content-length connu → pas de chunked)
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType("application/octet-stream")
                .contentLength((long) cipherText.length)
                .metadata(Map.of(
                        "orig-mime", (origMime != null ? origMime : "application/octet-stream"),
                        "enc", "AES-GCM",
                        "ver", "1"
                ))
                .build();

        try (var in = new java.io.ByteArrayInputStream(cipherText)) {
            s3.putObject(put, RequestBody.fromInputStream(in, cipherText.length));
        }

        return new EncryptedUploadResult(
                objectKey,
                EncryptionService.b64(iv),
                EncryptionService.b64(wrapped),
                cipherText.length
        );
    }

    public ResponseInputStream<GetObjectResponse> getObject(String key) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (S3Exception e) {
            // On ne fait pas échouer la requête pour ça : on log et on laisse un cleanup ultérieur si besoin
            log.warn("S3 delete failed for key {}: {}", objectKey, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
        }
    }
}