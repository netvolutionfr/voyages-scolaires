package fr.siovision.voyages.application.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private final SecretKey kek; // Master key pour wrap/unwrap
    private static final SecureRandom RNG = new SecureRandom();

    public EncryptionService(
            @Value("${app.crypto.kek.b64}") String kekBase64 // 16/24/32 octets en Base64
    ) {
        byte[] raw = Base64.getDecoder().decode(kekBase64);
        if (!(raw.length == 16 || raw.length == 24 || raw.length == 32)) {
            throw new IllegalArgumentException("app.crypto.kek.base64 doit faire 16/24/32 octets");
        }
        this.kek = new SecretKeySpec(raw, "AES");
    }

    /** Génère une DEK (AES-256). */
    public SecretKey generateDek() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, RNG);
            return kg.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** IV 12 octets pour AES/GCM. */
    public byte[] newIv() {
        byte[] iv = new byte[12];
        RNG.nextBytes(iv);
        return iv;
    }

    /** Chiffre des octets (AES/GCM/NoPadding). */
    public byte[] encryptAesGcm(byte[] plaintext, SecretKey dek, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, dek, new javax.crypto.spec.GCMParameterSpec(128, iv));
        return c.doFinal(plaintext);
    }

    /** Déchiffre (AES/GCM/NoPadding). */
    public byte[] decryptAesGcm(byte[] ciphertext, SecretKey dek, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, dek, new javax.crypto.spec.GCMParameterSpec(128, iv));
        return c.doFinal(ciphertext);
    }

    /** AES Key Wrap (RFC3394) — pas d’IV stocké. */
    public byte[] wrapDek(SecretKey dek) throws Exception {
        Cipher wrap = Cipher.getInstance("AESWrap");
        wrap.init(Cipher.WRAP_MODE, kek);
        return wrap.wrap(dek);
    }

    /** AES Key Unwrap. */
    public SecretKey unwrapDek(byte[] wrapped) throws Exception {
        Cipher unwrap = Cipher.getInstance("AESWrap");
        unwrap.init(Cipher.UNWRAP_MODE, kek);
        return (SecretKey) unwrap.unwrap(wrapped, "AES", Cipher.SECRET_KEY);
    }

    // Utils Base64
    public static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }
    public static byte[] b64d(String s) {
        return Base64.getDecoder().decode(s);
    }

    // SHA-256 util si besoin
    public String computeSha256(InputStream in) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) md.update(buf, 0, r);
            return Base64.getEncoder().encodeToString(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}