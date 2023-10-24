package uk.gov.hmcts.idam.userprofilebridge.error;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper;

import java.util.List;

import static uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper.convertJsonToMap;
import static uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper.extractMessagesFromMap;

@Component
@Slf4j
public class ListenerErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        if (t.getCause() != null && t.getCause() instanceof HttpStatusCodeException hsce) {
            String responseBody = hsce.getResponseBodyAsString();
            if (StringUtils.isNotEmpty(responseBody)) {
                log.warn("Listener led to http exception {};{};{}", hsce.getStatusCode(), hsce.getMessage(), responseBody);
            }
        } else {
            log.error("Listener led to exception", t);
        }
    }

}
