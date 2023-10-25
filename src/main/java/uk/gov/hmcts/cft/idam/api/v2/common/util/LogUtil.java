package uk.gov.hmcts.cft.idam.api.v2.common.util;

public class LogUtil {

    public static String obfuscateEmail(String email, int visibleChars) {
        if (email != null) {
            return email.replaceAll("(?<=.{" + visibleChars + "})[^/.](?=.*@)", "*");
        }
        return null;
    }

}
