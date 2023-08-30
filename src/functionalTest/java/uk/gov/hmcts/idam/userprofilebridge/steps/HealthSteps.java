package uk.gov.hmcts.idam.userprofilebridge.steps;

import io.cucumber.java.en.When;

public class HealthSteps extends BaseSteps {

    @When("get health")
    public void whenGetHealth() {
        given().get("/health");
    }

}
