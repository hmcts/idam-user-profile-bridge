package uk.gov.hmcts.idam.userprofilebridge;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.idam.userprofilebridge.steps.HealthSteps;

@ExtendWith(SerenityJUnit5Extension.class)
public class HealthApiTest {

    @Steps
    HealthSteps healthSteps;

    @Test
    public void test() {
        healthSteps.getHealth();
        healthSteps.thenStatusCodeIs(HttpStatus.OK);
    }

}
