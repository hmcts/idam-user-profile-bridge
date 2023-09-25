package uk.gov.hmcts.idam.userprofilebridge.steps;

import io.cucumber.java.en.Given;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.ServiceProvider;
import uk.gov.hmcts.idam.userprofilebridge.config.EnvConfig;

import java.util.List;

public class ServiceProviderSteps extends BaseSteps {

    @Given("a test service provider {0}")
    public ServiceProvider givenATestServiceProvider(String clientId, String clientSecret, List<String> scopes) {
        ServiceProvider serviceProvider = buildServiceProvider(clientId, clientSecret, scopes);
        return createServiceProvider(serviceProvider);
    }

    private ServiceProvider buildServiceProvider(String clientId, String clientSecret, List<String> scopes) {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setClientId(clientId);
        serviceProvider.setClientSecret(clientSecret);

        serviceProvider.setOAuth2(serviceProvider.new OAuth2());
        serviceProvider.setHmctsAccess(serviceProvider.new HmctsAccess());

        serviceProvider.getOAuth2().setScopes(scopes);
        serviceProvider.getOAuth2().setGrantTypes(List.of("client_credentials"));
        return serviceProvider;
    }

    private ServiceProvider createServiceProvider(ServiceProvider serviceProvider) {
        String token = getTestingServiceClientToken();
        return SerenityRest.given().baseUri(EnvConfig.TESTING_SUPPORT_API_URL)
            .header("authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(serviceProvider)
            .post("/test/idam/services")
            .then().assertThat().statusCode(HttpStatus.CREATED.value())
            .and().extract().as(ServiceProvider.class);
    }

}
