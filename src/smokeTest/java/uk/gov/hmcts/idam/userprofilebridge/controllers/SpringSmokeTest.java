package uk.gov.hmcts.idam.userprofilebridge.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://localhost:1234/dummy")
public class SpringSmokeTest {

    @Test
    public void contextLoads() throws Exception {
        assert true;
    }

}
