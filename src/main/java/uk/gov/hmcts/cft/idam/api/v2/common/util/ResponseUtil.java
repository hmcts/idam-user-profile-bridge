package uk.gov.hmcts.cft.idam.api.v2.common.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Optional;
import java.util.function.Supplier;

public class ResponseUtil {

    public static <R> Optional<R> asOptional(Supplier<R> function) {
        try {
            return Optional.ofNullable(function.get());
        } catch (HttpStatusCodeException hsce) {
            if (hsce.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw hsce;
        }
    }

}
