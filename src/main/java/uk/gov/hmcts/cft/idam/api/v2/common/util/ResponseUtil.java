package uk.gov.hmcts.cft.idam.api.v2.common.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ResponseUtil {

    private ResponseUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

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

    public static <R> R expectSingle(
        Supplier<List<R>> supplier,
        Function<List<R>, HttpStatusCodeException> onMultiple
    ) throws HttpStatusCodeException {
        List<R> result = supplier.get();

        if (result == null || result.isEmpty()) {
            throw SpringWebClientHelper.notFound();
        }

        if (result.size() > 1) {
            throw onMultiple.apply(result);
        }

        return result.getFirst();
    }

}
