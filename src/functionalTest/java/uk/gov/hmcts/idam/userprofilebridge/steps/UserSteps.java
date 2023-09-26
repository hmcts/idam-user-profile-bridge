package uk.gov.hmcts.idam.userprofilebridge.steps;

import io.cucumber.java.en.Given;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.ActivatedUserRequest;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.config.EnvConfig;

import java.util.List;
import java.util.UUID;

public class UserSteps extends BaseSteps {

    @Given("a test citizen {0}")
    public User givenATestCitizen(String email, String password) {
        User user = buildUser(email, List.of("citizen"));
        ActivatedUserRequest activatedUserRequest = new ActivatedUserRequest();
        activatedUserRequest.setUser(user);
        activatedUserRequest.setPassword(password);
        return createUser(activatedUserRequest);
    }

    private User buildUser(String email, List<String> roleNames) {
        String unique = RandomStringUtils.randomAlphabetic(8);
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setForename(unique);
        user.setSurname(unique);
        user.setRoleNames(roleNames);
        return user;
    }

    private User createUser(ActivatedUserRequest activatedUserRequest) {
        String token = getTestingServiceClientToken();
        return SerenityRest.given().baseUri(EnvConfig.TESTING_SUPPORT_API_URL)
            .header("authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(activatedUserRequest)
            .post("/test/idam/users")
            .then().assertThat().statusCode(HttpStatus.CREATED.value())
            .and().extract().as(User.class);
    }

}
