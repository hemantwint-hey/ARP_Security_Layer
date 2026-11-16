package org.example.arp_ecurity_layer.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AesGcmEncryptor {

    @Value("${arp.crypto.aes-secret-key}")
    private String secretKey;

    /**
     * Encrypts plain text using AES-GCM 256-bit encryption.
     * @param plainText the raw value to encrypt (e.g., customerId string)
     * @return Base64-encoded ciphertext with IV prepended
     */
    public String encrypt(String plainText) {
        // TODO: Use javax.crypto AES/GCM/NoPadding, random 12-byte IV, 256-bit key from secretKey
        throw new UnsupportedOperationException("TODO: implement");
    }

    /**
     * Decrypts AES-GCM encrypted ciphertext.
     * @param cipherText Base64-encoded ciphertext with IV prepended
     * @return original plain text
     */
    public String decrypt(String cipherText) {
        // TODO: Extract IV from first 12 bytes, decrypt remainder with AES/GCM/NoPadding
        throw new UnsupportedOperationException("TODO: implement");
    }
}
