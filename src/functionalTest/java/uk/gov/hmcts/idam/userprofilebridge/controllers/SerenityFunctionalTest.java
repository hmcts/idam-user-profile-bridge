package uk.gov.hmcts.idam.userprofilebridge.controllers;

import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SerenityJUnit5Extension.class)
public class SerenityFunctionalTest {

    @Steps
    SimpleSteps simpleSteps;

    @Test
    public void test() {
        simpleSteps.thenOkay();
    }

}
