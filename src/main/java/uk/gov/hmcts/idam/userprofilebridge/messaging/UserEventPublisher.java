package uk.gov.hmcts.idam.userprofilebridge.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;

import java.time.LocalDateTime;

@Slf4j
@Component
public class UserEventPublisher {

    public static final String MODIFY_USER_DESTINATION = "idam-modify-user";

    public static final String ADD_USER_DESTINATION = "idam-add-user";

    private final JmsTemplate jmsTemplate;

    public UserEventPublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publish(User user, EventType eventType, String clientId) {
        UserEvent event = buildEvent(user, eventType, clientId);
        if (event.getEventType() == EventType.MODIFY) {
            safePublish(MODIFY_USER_DESTINATION, event);
        } else if (event.getEventType() == EventType.ADD) {
            safePublish(ADD_USER_DESTINATION, event);
        } else {
            log.warn("No destination for event type {}", eventType);
        }
    }

    private void safePublish(String destination, UserEvent event) {
        try {
            jmsTemplate.convertAndSend(destination, event);
        } catch (Exception e) {
            log.warn("Exception publishing to {}", destination, e);
        }
    }

    private UserEvent buildEvent(User user, EventType eventType, String clientId) {
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(eventType);
        userEvent.setClientId(clientId);
        userEvent.setEventDateTime(LocalDateTime.now());
        return userEvent;
    }

}
