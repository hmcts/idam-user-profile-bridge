package uk.gov.hmcts.idam.userprofilebridge.steps;

import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.cft.rd.model.CaseWorkerProfile;
import uk.gov.hmcts.cft.rd.model.UserProfile;

public class BridgeSteps extends BaseSteps {

    @When("Get user with id {1}")
    public User getUser(String bearerToken, String userId) {
        return given().accept(ContentType.JSON)
            .header("authorization", "Bearer " + bearerToken)
            .get("idam/api/v2/users/" + userId).then().extract().as(User.class);
    }

    @When("Get user profile with id {1}")
    public UserProfile getUserProfile(String bearerToken, String userId) {
        return given().accept(ContentType.JSON)
            .header("authorization", "Bearer " + bearerToken)
            .get("rd/api/v1/userprofile/" + userId).then().extract().as(UserProfile.class);
    }

    @When("Get caseworker profile with id {1}")
    public CaseWorkerProfile getCaseWorkerProfile(String bearerToken, String userId) {
        return given().accept(ContentType.JSON)
            .header("authorization", "Bearer " + bearerToken)
            .get("rd/case-worker/profile/search-by-id/" + userId).then().extract().as(CaseWorkerProfile.class);
    }

    @When("Synchronise user with id {1}")
    public void synchroniseUser(String bearerToken, String userId) {
        given()
            .header("authorization", "Bearer " + bearerToken)
            .put("bridge/user/" + userId)
            .then().assertThat().statusCode(HttpStatus.OK.value());
    }
}
