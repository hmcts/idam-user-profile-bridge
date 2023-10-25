package uk.gov.hmcts.idam.userprofilebridge.messaging;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.service.UserEventService;
import uk.gov.hmcts.idam.userprofilebridge.trace.TraceAttribute;

@Slf4j
@Component
public class UserEventListener {

    private final UserEventService userEventService;

    public UserEventListener(UserEventService userEventService) {
        this.userEventService = userEventService;
    }

    @JmsListener(destination = "${idam.messaging.subscription.modify-user}", containerFactory =
        "jmsListenerContainerFactory")
    public void receiveModifyUserEvent(UserEvent event) {
        handleEvent(event);
    }

    @JmsListener(destination = "${idam.messaging.subscription.add-user}", containerFactory =
        "jmsListenerContainerFactory")
    public void receiveAddUserEvent(UserEvent event) {
        handleEvent(event);
    }

    public void handleEvent(UserEvent event) {
        Span.current().setAttribute(TraceAttribute.USER_ID, event.getUser().getId());
        try {
            userEventService.handle(event);
        } catch (HttpStatusCodeException hsce) {
            Span.current().setAttribute(TraceAttribute.ERROR, hsce.getStatusCode() + "; " + hsce.getMessage());
            String responseBody = hsce.getResponseBodyAsString();
            if (StringUtils.isNotEmpty(responseBody)) {
                log.warn("Listener led to http exception {};{};{}",
                         hsce.getStatusCode(),
                         hsce.getMessage(),
                         responseBody
                );
            }
            throw hsce;
        }
    }

}
