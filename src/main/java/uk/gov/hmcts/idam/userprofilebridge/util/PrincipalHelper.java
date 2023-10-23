package uk.gov.hmcts.idam.userprofilebridge.util;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.Optional;

public class PrincipalHelper {

    private PrincipalHelper() {
    }

    /**
     * Get client id.
     * @should return first client id if present
     * @should return optional empty if no client id is present
     */
    public static Optional<String> getClientId(Jwt principal) {
        return Optional.ofNullable(principal.getClaimAsStringList("aud"))
            .orElse(Collections.emptyList())
            .stream().findFirst();
    }

}
