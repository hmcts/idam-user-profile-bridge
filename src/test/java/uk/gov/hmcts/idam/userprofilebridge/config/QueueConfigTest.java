package uk.gov.hmcts.idam.userprofilebridge.config;

import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.support.converter.MessageConverter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueConfigTest {

    private final MessageConverter converter = new QueueConfig().jacksonJmsMessageConverter();

    @Test
    void writesExistingEventTypeAndDateFormats() throws Exception {
        Session session = mock(Session.class);
        TextMessage message = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(message);
        User user = new User();
        user.setId("user-id");
        user.setCreateDate(ZonedDateTime.parse("2026-07-01T12:30:00+01:00[Europe/London]"));
        UserEvent event = new UserEvent();
        event.setEventType(EventType.ADD);
        event.setEventDateTime(LocalDateTime.parse("2026-07-01T12:30:00"));
        event.setUser(user);

        converter.toMessage(event, session);

        verify(message).setStringProperty("_type", "idam.userevent");
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(session).createTextMessage(json.capture());
        JsonNode payload = JsonMapper.builder().build().readTree(json.getValue());
        assertEquals("ADD", payload.get("eventType").asText());
        assertEquals("2026-07-01T12:30:00", payload.get("eventDateTime").asText());
        assertEquals("2026-07-01T12:30:00+01:00[Europe/London]",
                     payload.get("user").get("createDate").asText());
    }

    @Test
    void readsExistingEventsWithUnknownFields() throws Exception {
        TextMessage message = mock(TextMessage.class);
        when(message.getStringProperty("_type")).thenReturn("idam.userevent");
        when(message.getText()).thenReturn("""
            {
              "eventType": "MODIFY",
              "eventDateTime": "2026-07-01T12:30:00",
              "futureEventField": "ignored",
              "user": {
                "id": "user-id",
                "createDate": "2026-07-01T12:30:00+01:00[Europe/London]",
                "futureUserField": "ignored"
              }
            }
            """);

        UserEvent event = (UserEvent) converter.fromMessage(message);

        assertEquals(EventType.MODIFY, event.getEventType());
        assertEquals(LocalDateTime.parse("2026-07-01T12:30:00"), event.getEventDateTime());
        assertEquals("user-id", event.getUser().getId());
        assertEquals(ZonedDateTime.parse("2026-07-01T12:30:00+01:00[Europe/London]").toInstant(),
                     event.getUser().getCreateDate().toInstant());
    }
}
