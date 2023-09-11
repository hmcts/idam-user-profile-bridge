package uk.gov.hmcts.idam.userprofilebridge.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;

import java.time.LocalDateTime;

import static uk.gov.hmcts.idam.userprofilebridge.messaging.UserEventListener.ADD_USER_DESTINATION;
import static uk.gov.hmcts.idam.userprofilebridge.messaging.UserEventListener.MODIFY_USER_DESTINATION;

@Slf4j
@Component
public class UserEventPublisher {

    private final JmsTemplate jmsTemplate;

    public UserEventPublisher(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publish(User user, EventType eventType, String clientId) {
        UserEvent event = buildEvent(user, eventType, clientId);
        if (event.getEventType() == EventType.MODIFY) {
            jmsTemplate.convertAndSend(MODIFY_USER_DESTINATION, event);
        } else if (event.getEventType() == EventType.ADD) {
            jmsTemplate.convertAndSend(ADD_USER_DESTINATION, event);
        } else {
            log.warn("No destination for event type {}", eventType);
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
