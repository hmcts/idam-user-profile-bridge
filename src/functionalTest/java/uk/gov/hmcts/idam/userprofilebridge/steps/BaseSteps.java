package uk.gov.hmcts.idam.userprofilebridge.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;
import uk.gov.hmcts.idam.userprofilebridge.config.EnvConfig;

import java.util.List;

public abstract class BaseSteps {

    private static String testingServiceClientToken;

    public RequestSpecification given() {
        return SerenityRest.given().baseUri(EnvConfig.BRIDGE_API_URL);
    }

    @Given("a testing service token")
    public String givenTestingServiceClientToken() {
        if (testingServiceClientToken == null) {
            testingServiceClientToken = SerenityRest.given().baseUri(EnvConfig.PUBLIC_URL)
                .contentType(ContentType.URLENC)
                .queryParam("client_id", EnvConfig.TESTING_SERVICE_CLIENT)
                .queryParam("client_secret", EnvConfig.TESTING_SERVICE_CLIENT_SECRET)
                .queryParam("scope", "profile")
                .queryParam("grant_type", "client_credentials").post("/o/token")
                .then().assertThat().statusCode(HttpStatus.OK.value())
                .and().extract().response().path("access_token");
        }
        return testingServiceClientToken;
    }

    protected String getTestingServiceClientToken() {
        return testingServiceClientToken;
    }

    @Given("a random email")
    public String givenRandomEmail() {
        return RandomStringUtils.randomAlphabetic(12) + "@functional.local";
    }

    @Given("a random password")
    public String givenRandomPassword() {
        return RandomStringUtils.randomAlphabetic(12) + "!2";
    }


    @Then("status code is {0}")
    public void thenStatusCodeIs(HttpStatus statusCode) {
        SerenityRest.then().assertThat().statusCode(statusCode.value());
    }

    @Given("a client credentials access token")
    public String givenAClientCredentialsAccessToken(String clientId, String clientSecret, List<String> scopes) {
        return SerenityRest.given().baseUri(EnvConfig.PUBLIC_URL)
            .contentType(ContentType.URLENC)
            .queryParam("client_id", clientId)
            .queryParam("client_secret", clientSecret)
            .queryParam("scope", CollectionUtils.isNotEmpty(scopes) ? String.join(" ", scopes) : "")
            .queryParam("grant_type", "client_credentials")
            .post("/o/token")
            .then().extract().response().path("access_token");
    }

    @Then("sleep for {0}ms")
    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            System.out.println(ie.getMessage());
        }
    }

}
