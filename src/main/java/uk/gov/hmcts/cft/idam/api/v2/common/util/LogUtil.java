package uk.gov.hmcts.cft.idam.api.v2.common.util;

public class LogUtil {

    private LogUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String obfuscateEmail(String email, int visibleChars) {
        if (email != null) {
            return email.replaceAll("(?<=.{" + visibleChars + "})[^/.](?=.*@)", "*");
        }
        return null;
    }

}
