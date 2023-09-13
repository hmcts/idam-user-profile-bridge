package uk.gov.hmcts.idam.userprofilebridge.steps;

import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;

public class BridgeSteps extends BaseSteps {

    @When("Get user with id {0}")
    public User getUser(String bearerToken, String userId) {
        return given().accept(ContentType.JSON)
            .header("authorization", "Bearer " + bearerToken)
            .get("idam/api/v2/users/" + userId).then().extract().as(User.class);
    }

}
