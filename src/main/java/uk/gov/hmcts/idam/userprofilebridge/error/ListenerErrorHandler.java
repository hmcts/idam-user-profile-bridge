package uk.gov.hmcts.idam.userprofilebridge.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;

@Component
@Slf4j
public class ListenerErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        if (t.getCause() != null && t.getCause() instanceof HttpStatusCodeException) {
            log.debug(
                "Listener led to HttpStatusCodeException exception, {}; {}",
                ((HttpStatusCodeException) t.getCause()).getStatusCode(),
                t.getCause().getMessage()
            );
        } else {
            log.error("Listener led to exception", t);
        }
    }

}
