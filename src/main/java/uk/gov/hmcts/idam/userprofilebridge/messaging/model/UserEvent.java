package uk.gov.hmcts.idam.userprofilebridge.messaging.model;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserEvent {

    EventType eventType;
    String clientId;
    User user;
    LocalDateTime eventDateTime;

}
