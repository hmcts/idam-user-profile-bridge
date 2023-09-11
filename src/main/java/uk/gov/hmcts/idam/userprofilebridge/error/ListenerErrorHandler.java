package uk.gov.hmcts.idam.userprofilebridge.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;

@Component
@Slf4j
public class ListenerErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        if (t instanceof HttpStatusCodeException
            && ((HttpStatusCodeException)t).getStatusCode() == HttpStatus.NOT_FOUND) {
            log.warn("Listener led to NOT_FOUND exception, {}", t.getMessage());
        }
        log.warn("Listener led to exception", t);
    }

}
