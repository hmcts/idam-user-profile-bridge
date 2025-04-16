package uk.gov.hmcts.idam.userprofilebridge.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "idam.bridge")
public class IdamBridgeProperties {

    List<String> excludedClients;
}
