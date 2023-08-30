package uk.gov.hmcts.idam.userprofilebridge;

import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.thucydides.core.annotations.Steps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.idam.userprofilebridge.steps.HealthSteps;

@ExtendWith(SerenityJUnit5Extension.class)
public class HealthFunctionalTest {

    @Steps
    private HealthSteps healthSteps;

    @Test
    void functionalTest() {
        healthSteps.whenGetHealth();
        healthSteps.thenStatusCodeIs(HttpStatus.OK);
    }

}
