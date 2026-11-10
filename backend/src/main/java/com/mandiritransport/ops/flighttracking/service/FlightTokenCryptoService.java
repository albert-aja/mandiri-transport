package com.mandiritransport.ops.flighttracking.service;

import com.mandiritransport.ops.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Application-level AES-256-GCM encryption service for FlightAware tracking credentials.
 * Ensures tracking tokens and user tokens are never stored in plaintext, never logged,
 * and never exposed in API responses.
 */
@Service
public class FlightTokenCryptoService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String VERSION_PREFIX = "v1:";

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public FlightTokenCryptoService(
            @Value("${app.flight-tracking.encryption-key:mandiri-transport-dev-flight-token-aes256-key-change-in-prod}")
            String rawKeyMaterial) {
        if (rawKeyMaterial == null || rawKeyMaterial.isBlank()) {
            throw new IllegalArgumentException("Flight tracking encryption key must not be blank");
        }
        this.secretKey = deriveAes256Key(rawKeyMaterial.trim());
    }

    private SecretKeySpec deriveAes256Key(String secret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES-256 key derivation", e);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw BusinessException.badRequest("TOKEN_ENCRYPTION_FAILED", "Gagal mengenkripsi token pelacakan penerbangan.");
        }
    }

    public String decrypt(String encryptedPayload) {
        if (encryptedPayload == null || encryptedPayload.isBlank()) {
            return null;
        }
        try {
            String base64Part = encryptedPayload.startsWith(VERSION_PREFIX)
                    ? encryptedPayload.substring(VERSION_PREFIX.length())
                    : encryptedPayload;
            byte[] decoded = Base64.getDecoder().decode(base64Part);
            if (decoded.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted token payload length");
            }

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw BusinessException.badRequest("TOKEN_DECRYPTION_FAILED", "Gagal mendekripsi token pelacakan (kunci tidak cocok atau data rusak).");
        }
    }

    /**
     * Generates a non-reversible SHA-256 fingerprint (`sha256:16-hex-chars`) so credentials
     * can be audited and compared without ever revealing or logging the raw token.
     */
    public String fingerprint(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return "none";
        }
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(plaintext.trim().getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception e) {
            return "sha256:unknown";
        }
    }
}
