package uk.gov.hmcts.idam.userprofilebridge.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.service.UserEventService;

@Slf4j
@Component
public class UserEventListener {

    private final UserEventService userEventService;

    public UserEventListener(UserEventService userEventService) {
        this.userEventService = userEventService;
    }

    @JmsListener(destination = "${idam.messaging.subscription.modify-user}", containerFactory = "jmsListenerContainerFactory")
    public void receiveModifyUserEvent(UserEvent event) {
        userEventService.handleModifyUserEvent(event);
    }

    @JmsListener(destination = "${idam.messaging.subscription.add-user}", containerFactory = "jmsListenerContainerFactory")
    public void receiveAddUserEvent(UserEvent event) {
        userEventService.handleAddUserEvent(event);
    }

}
