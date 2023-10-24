package uk.gov.hmcts.idam.userprofilebridge.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.service.UserEventService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
        userEvent.setUser(new User());
        userEvent.getUser().setId("test-id");
        userEventListener.receiveModifyUserEvent(userEvent);
        verify(userEventService, times(1)).handleModifyUserEvent(userEvent);
    }

    @Test
    public void receiveModifyUserEvent_withException() {
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(new User());
        userEvent.getUser().setId("test-id");
        doThrow(SpringWebClientHelper.exception(HttpStatus.I_AM_A_TEAPOT, new RuntimeException()))
            .when(userEventService)
            .handleModifyUserEvent(any());
        try {
            userEventListener.receiveModifyUserEvent(userEvent);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.I_AM_A_TEAPOT, hsce.getStatusCode());
        }
    }

    @Test
    public void receiveAddUserEvent() {
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(new User());
        userEvent.getUser().setId("test-id");
        userEventListener.receiveAddUserEvent(userEvent);
        verify(userEventService, times(1)).handleAddUserEvent(userEvent);
    }

    @Test
    public void receiveAddUserEvent_withException() {
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(new User());
        userEvent.getUser().setId("test-id");
        doThrow(SpringWebClientHelper.exception(HttpStatus.I_AM_A_TEAPOT, new RuntimeException()))
            .when(userEventService)
            .handleAddUserEvent(any());
        try {
            userEventListener.receiveAddUserEvent(userEvent);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.I_AM_A_TEAPOT, hsce.getStatusCode());
        }
    }

}
