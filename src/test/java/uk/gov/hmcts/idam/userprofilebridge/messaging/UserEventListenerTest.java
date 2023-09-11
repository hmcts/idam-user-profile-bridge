package uk.gov.hmcts.idam.userprofilebridge.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.service.UserEventService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    UserEventService userEventService;

    @InjectMocks
    UserEventListener userEventListener;

    @Test
    public void receiveModifyUserEvent() {
        UserEvent userEvent = new UserEvent();
        userEventListener.receiveModifyUserEvent(userEvent);
        verify(userEventService, times(1)).handleModifyUserEvent(userEvent);
    }

    @Test
    public void receiveAddUserEvent() {
        UserEvent userEvent = new UserEvent();
        userEventListener.receiveAddUserEvent(userEvent);
        verify(userEventService, times(1)).handleAddUserEvent(userEvent);
    }

}
