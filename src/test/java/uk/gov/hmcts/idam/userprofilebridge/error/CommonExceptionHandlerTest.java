package uk.gov.hmcts.idam.userprofilebridge.error;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.model.ApiError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CommonExceptionHandlerTest {

    @InjectMocks
    CommonExceptionHandler underTest;

    @Test
    public void handle() {
        HttpStatusCodeException hsce = mock(HttpStatusCodeException.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(hsce.getStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT);
        given(request.getMethod()).willReturn("test-method");
        given(request.getRequestURI()).willReturn("test-uri");
        ResponseEntity<ApiError> result = underTest.handle(hsce, request);
        ApiError resultbody = result.getBody();
        assertEquals("test-method", resultbody.getMethod());
        assertEquals("test-uri", resultbody.getPath());
        assertEquals(418, resultbody.getStatus());
    }

    @Test
    public void handleAccessDeniedException() {
        AccessDeniedException ade = mock(AccessDeniedException.class);
        given(ade.getMessage()).willReturn("test-error");
        try {
            underTest.handleAccessDeniedException(ade);
            fail();
        } catch (AccessDeniedException exc) {
            assertEquals(ade, exc);
        }
    }

}
