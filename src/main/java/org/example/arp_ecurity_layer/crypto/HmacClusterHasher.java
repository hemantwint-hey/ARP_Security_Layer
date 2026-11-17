package org.example.arp_ecurity_layer.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class HmacClusterHasher {
    private static final String HMAC_SHA256  = "HmacSHA256";

    @Value("${arp.crypto.hmac-salt}")
    private String hmacSalt;

    /**
     * Produces a salted HMAC-SHA256 hash of a merchant descriptor for clustering similar merchants.
     * Normalizes input (lowercase, trim, strip punctuation) before hashing.
     * @param rawDescriptor raw merchant name or transaction remarks snippet
     * @return hex-encoded HMAC-SHA256 hash (64 chars)
     */
    public String hash(String rawDescriptor) {
        if (rawDescriptor == null || rawDescriptor.isBlank()) return "UNKNOWN";
        try {
            String normalized = rawDescriptor.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(hmacSalt.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hmacByte = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));

            // convert to 64 - char Hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacByte) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append("0");
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
}
