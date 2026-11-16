package org.example.arp_ecurity_layer.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HmacClusterHasher {

    @Value("${arp.crypto.hmac-salt}")
    private String hmacSalt;

    /**
     * Produces a salted HMAC-SHA256 hash of a merchant descriptor for clustering similar merchants.
     * Normalizes input (lowercase, trim, strip punctuation) before hashing.
     * @param rawDescriptor raw merchant name or transaction remarks snippet
     * @return hex-encoded HMAC-SHA256 hash (64 chars)
     */
    public String hash(String rawDescriptor) {
        // TODO: Normalize rawDescriptor (lowercase, trim, strip non-alphanum except spaces)
        // TODO: Compute HMAC-SHA256 with hmacSalt as key
        // TODO: Return hex-encoded result
        throw new UnsupportedOperationException("TODO: implement");
    }
}
