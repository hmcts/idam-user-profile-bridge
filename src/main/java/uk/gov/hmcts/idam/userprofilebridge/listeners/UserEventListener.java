package uk.gov.hmcts.idam.userprofilebridge.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.idam.userprofilebridge.listeners.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.service.UserEventService;

@Slf4j
@Component
public class UserEventListener {

    public static final String MODIFY_USER_DESTINATION = "modify-user";

    private final UserEventService userEventService;

    public UserEventListener(UserEventService userEventService) {
        this.userEventService = userEventService;
    }

    @JmsListener(destination = MODIFY_USER_DESTINATION)
    public void receiveModifyUserEvent(UserEvent event) {
        userEventService.handleModifyUserEvent(event);
    }

}
