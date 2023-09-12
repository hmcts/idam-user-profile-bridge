package uk.gov.hmcts.idam.userprofilebridge.steps;

import io.cucumber.java.en.When;
import io.restassured.response.Response;

public class HealthSteps extends BaseSteps {

    @When("get health")
    public Response getHealth() {
        return given().get("/health").then().extract().response();
    }

}
