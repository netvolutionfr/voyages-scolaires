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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Génère une clé S3 sécurisée et non révélatrice de données personnelles.
     * Exemple de résultat :
     *   docs/2f4d9c53-8c8b-4d7f-a4f8-b5f14cb8bb28/8c37d781-9c1a-46fd-8a4e-13fd4b1c9b92.pdf
     */
    public String buildEncryptedKey(UUID userPublicId, String originalFilename) {
        // Extraire l’extension en conservant le point (".pdf", ".jpg", etc.)
        String extension = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                extension = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
                // On limite aux extensions ASCII, pour éviter les caractères exotiques
                if (!extension.matches("^\\.[a-z0-9]{1,6}$")) {
                    extension = "";
                }
            }
        }

        // Dossier par utilisateur (UUID stable)
        String userFolder = (userPublicId != null ? userPublicId.toString() : "anonymous");

        // Nom technique unique (UUID v4)
        String randomName = UUID.randomUUID().toString();

        return String.format("docs/%s/%s%s", userFolder, randomName, extension);
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