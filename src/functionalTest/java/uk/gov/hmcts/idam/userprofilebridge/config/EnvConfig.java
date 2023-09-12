package uk.gov.hmcts.idam.userprofilebridge.config;

public class EnvConfig {

    private EnvConfig() {
    }

    public static final String BRIDGE_API_URL = System.getenv("TEST_URL");
    
    public static final String PUBLIC_URL = System.getenv("PUBLIC_URL");

    public static final String TESTING_SUPPORT_API_URL = System.getenv("TESTING_SUPPORT_API_URL");

    public static final String TESTING_SERVICE_CLIENT = System.getenv("TESTING_SERVICE_CLIENT");

    public static final String TESTING_SERVICE_CLIENT_SECRET = System.getenv("TESTING_SERVICE_CLIENT_SECRET");

}
