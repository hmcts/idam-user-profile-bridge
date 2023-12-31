package uk.gov.hmcts.idam.userprofilebridge.error;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.cft.idam.api.v2.common.model.ApiError;
import uk.gov.hmcts.idam.userprofilebridge.trace.TraceAttribute;

import java.time.Instant;
import java.util.Collections;

@ControllerAdvice
@Slf4j
public class CommonExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Convert http status code exception to error response.
     * @should convert HttpStatusCodeException to error response with single message
     * @should convert HttpStatusCodeException to error response with details from body
     */
    @ExceptionHandler
    public ResponseEntity<ApiError> handle(final HttpStatusCodeException hsce, final HttpServletRequest request) {

        ApiError apiError = new ApiError();
        apiError.setTimestamp(Instant.now());
        apiError.setStatus(hsce.getStatusCode().value());
        apiError.setMethod(request.getMethod());
        apiError.setPath(request.getRequestURI());

        apiError.setErrors(Collections.singletonList(hsce.getMessage()));

        return ResponseEntity.status(hsce.getStatusCode()).body(apiError);

    }

    /**
     * Re-throw {@link AccessDeniedException} and let
     * {@link org.springframework.security.web.access.AccessDeniedHandler} deal with it
     */
    @ExceptionHandler
    protected void handleAccessDeniedException(AccessDeniedException e) {
        if (e.getMessage() != null && !"Access is denied".equals(e.getMessage())) {
            Span.current().setAttribute(TraceAttribute.ERROR, "permission_failure: " + e.getMessage());
        }
        throw e;
    }

}
