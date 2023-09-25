package uk.gov.hmcts.idam.userprofilebridge;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.ServiceProvider;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.steps.BridgeSteps;
import uk.gov.hmcts.idam.userprofilebridge.steps.ServiceProviderSteps;
import uk.gov.hmcts.idam.userprofilebridge.steps.UserSteps;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SerenityJUnit5Extension.class)
public class UserApiTest {

    @Steps
    UserSteps userSteps;

    @Steps
    ServiceProviderSteps serviceProviderSteps;

    @Steps
    BridgeSteps bridgeSteps;

    private String bridgeAccessToken;

    private ServiceProvider bridgeAccessService;

    @BeforeEach
    public void setup() {
        userSteps.givenTestingServiceClientToken();
        if (bridgeAccessService == null) {
            String clientId = "bridge-test-" + RandomStringUtils.randomAlphabetic(8);
            ;
            String clientSecret = userSteps.givenRandomPassword();
            bridgeAccessService = serviceProviderSteps.givenATestServiceProvider(clientId,
                                                                                 clientSecret,
                                                                                 List.of("profile",
                                                                                     "view-user-profile",
                                                                                     "sync-user-profile"));
            bridgeAccessService.setClientSecret(clientSecret);
        }
        if (bridgeAccessToken == null) {
            bridgeAccessToken = bridgeSteps.givenAClientCredentialsAccessToken(bridgeAccessService.getClientId(),
                                                                               bridgeAccessService.getClientSecret(),
                                                                               List.of("view-user-profile")
            );
        }
    }

    @Test
    @Title("Get IDAM user successfully")
    public void testGetIdamUser() {
        User testUser = userSteps.givenATestCitizen(userSteps.givenRandomEmail(), userSteps.givenRandomPassword());
        User result = bridgeSteps.getUser(bridgeAccessToken, testUser.getId());
        bridgeSteps.thenStatusCodeIs(HttpStatus.OK);
        assertThat(result.getEmail(), is(testUser.getEmail()));
    }

}
