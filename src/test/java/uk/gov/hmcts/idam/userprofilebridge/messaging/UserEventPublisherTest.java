package uk.gov.hmcts.idam.userprofilebridge.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.idam.userprofilebridge.messaging.UserEventPublisher.ADD_USER_DESTINATION;

@ExtendWith(MockitoExtension.class)
class UserEventPublisherTest {

    @Mock
    JmsTemplate jmsTemplate;

    @InjectMocks
    UserEventPublisher underTest;

    @Captor
    ArgumentCaptor<UserEvent> userEventArgumentCaptor;

    @Test
    public void publish() {
        User user = new User();
        underTest.publish(user, EventType.ADD, "test-client-id");
        verify(jmsTemplate, times(1)).convertAndSend(eq(ADD_USER_DESTINATION), userEventArgumentCaptor.capture());
        UserEvent userEvent = userEventArgumentCaptor.getValue();
        assertEquals(user, userEvent.getUser());
        assertEquals(EventType.ADD, userEvent.getEventType());
        assertEquals("test-client-id", userEvent.getClientId());
        assertNotNull(userEvent.getEventDateTime());
    }

    @Test
    public void publish_withException() {
        User user = new User();
        doThrow(SpringWebClientHelper
                    .exception(HttpStatus.I_AM_A_TEAPOT, "test-message", new HttpHeaders(), "test-body".getBytes())
                    .orElse(new RuntimeException())).when(jmsTemplate).convertAndSend(anyString(), any(UserEvent.class));
        underTest.publish(user, EventType.ADD, "test-client-id");

    }

}
