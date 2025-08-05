package fr.siovision.voyages.infrastructure.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDate;
import java.util.Base64;

@Converter
public class CryptoDateConverter implements AttributeConverter<LocalDate, String> {
    private static final String ALGO = "AES";
    private static final String SECRET = "MySecretKey12345"; // 16 chars pour AES-128

    private Cipher getCipher(int mode) throws Exception {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), ALGO);
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(mode, key);
        return cipher;
    }

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.toString().getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de chiffrement", e);
        }
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
            String dateString = new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de déchiffrement", e);
        }
    }
}
