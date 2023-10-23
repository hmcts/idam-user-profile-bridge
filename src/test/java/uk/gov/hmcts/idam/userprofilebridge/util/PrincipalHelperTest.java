package uk.gov.hmcts.idam.userprofilebridge.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PrincipalHelperTest {

    @Mock
    Jwt principal;

    /**
     * @verifies return first client id if present
     * @see PrincipalHelper#getClientId(Jwt)
     */
    @Test
    public void getClientId_shouldReturnFirstClientIdIfPresent() throws Exception {
        when(principal.getClaimAsStringList("aud")).thenReturn(Collections.singletonList("test-client"));
        assertEquals("test-client", PrincipalHelper.getClientId(principal).get());
        when(principal.getClaimAsStringList("aud")).thenReturn(Arrays.asList("test-client1", "test-client2"));
        assertEquals("test-client1", PrincipalHelper.getClientId(principal).get());
    }

    /**
     * @verifies return optional empty if no client id is present
     * @see PrincipalHelper#getClientId(Jwt)
     */
    @Test
    public void getClientId_shouldReturnOptionalEmptyIfNoClientIdIsPresent() throws Exception {
        when(principal.getClaimAsStringList("aud")).thenReturn(null);
        assertTrue(PrincipalHelper.getClientId(principal).isEmpty());
        when(principal.getClaimAsStringList("aud")).thenReturn(Collections.emptyList());
        assertTrue(PrincipalHelper.getClientId(principal).isEmpty());
    }
}
