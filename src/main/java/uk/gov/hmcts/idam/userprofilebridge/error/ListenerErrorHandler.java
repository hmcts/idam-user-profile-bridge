package uk.gov.hmcts.idam.userprofilebridge.error;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.idam.userprofilebridge.trace.TraceAttribute;

@Component
@Slf4j
public class ListenerErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        if (t instanceof HttpStatusCodeException
            && ((HttpStatusCodeException) t).getStatusCode() == HttpStatus.NOT_FOUND) {
            Span.current().setAttribute(TraceAttribute.ERROR, "Listener led to NOT_FOUND exception");
            log.warn("Listener led to NOT_FOUND exception, {}", t.getMessage());
        } else {
            Span.current().setAttribute(
                TraceAttribute.ERROR,
                "Listener led to exception: " + t.getClass() + ": " + t.getMessage()
            );
            log.warn("Listener led to exception", t);
        }
    }

}
