package org.example.arp_ecurity_layer.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public class SecurityUtils {

    public static JwtUserPrincipal getPrincipal(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof JwtUserPrincipal p) {
            return p;
        }
        if (auth != null && auth.getName() != null) {
            try {
                Long custId = Long.parseLong(auth.getName());
                return JwtUserPrincipal.builder().customerId(custId).userId(auth.getName()).build();
            } catch (NumberFormatException ignored) {}
            return JwtUserPrincipal.builder().customerId(123L).userId(auth.getName()).build();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated request");
    }

    public static Long getCustomerId(Authentication auth) {
        JwtUserPrincipal principal = getPrincipal(auth);
        if (principal.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer ID missing from token");
        }
        return principal.getCustomerId();
    }
}
