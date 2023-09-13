package uk.gov.hmcts.idam.userprofilebridge;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Title;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.config.EnvConfig;
import uk.gov.hmcts.idam.userprofilebridge.steps.BridgeSteps;
import uk.gov.hmcts.idam.userprofilebridge.steps.UserSteps;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SerenityJUnit5Extension.class)
public class UserApiTest {

    @Steps
    UserSteps userSteps;

    @Steps
    BridgeSteps bridgeSteps;

    private String bridgeServiceToken;

    @BeforeEach
    public void setup() {
        userSteps.givenTestingServiceClientToken();
        if (bridgeServiceToken == null) {
            bridgeServiceToken = bridgeSteps.givenAClientCredentialsAccessToken(
                EnvConfig.TESTING_SERVICE_CLIENT,
                EnvConfig.TESTING_SERVICE_CLIENT_SECRET,
                List.of("profile"));
        }
    }

    @Test
    @Title("Get IDAM user successfully")
    public void testGetIdamUser() {
        User testUser = userSteps.givenATestCitizen(userSteps.givenRandomEmail(), userSteps.givenRandomPassword());
        User result = bridgeSteps.getUser(bridgeServiceToken, testUser.getId());
        bridgeSteps.thenStatusCodeIs(HttpStatus.OK);
        assertThat(result.getEmail(), is(testUser.getEmail()));
    }

}
