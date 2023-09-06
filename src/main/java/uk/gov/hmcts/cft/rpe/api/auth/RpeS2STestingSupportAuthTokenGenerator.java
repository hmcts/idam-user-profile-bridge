package uk.gov.hmcts.cft.rpe.api.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cft.rpe.api.RpeS2STestingSupportApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Optional;

@ConditionalOnProperty(value = "featureFlags.s2sTestingSupportEnabled", havingValue = "true", matchIfMissing = false)
@Component
public class RpeS2STestingSupportAuthTokenGenerator implements AuthTokenGenerator {

    private static final Object TOKEN_KEY = "rpeS2STestingSupportLeasedToken";
    private static final String CACHE_NAME = "tokencaches2s";
    private final String serviceName;
    private final RpeS2STestingSupportApi rpeS2STestingSupportApi;
    private String currentLeasedToken;
    private Long currentLeasedTokenExpiryTimestamp;
    private final static Long TTL_MS = 5L * 60 * 1000;

    public RpeS2STestingSupportAuthTokenGenerator(String serviceName, RpeS2STestingSupportApi rpeS2STestingSupportApi) {
        this.serviceName = serviceName;
        this.rpeS2STestingSupportApi = rpeS2STestingSupportApi;
    }

    @Override
    public String generate() {
        return findTokenInCache()
            .orElseGet(() -> putInCache(getValueFromTestingSupport()));
    }

    public String getValueFromTestingSupport() {
        return rpeS2STestingSupportApi.lease(serviceName);
    }

    private String putInCache(String value) {
        if (StringUtils.isNotEmpty(value)) {
            currentLeasedToken = value;
            currentLeasedTokenExpiryTimestamp = System.currentTimeMillis() + TTL_MS;
        } else {
            currentLeasedToken = null;
            currentLeasedTokenExpiryTimestamp = 0L;
        }
        return value;
    }

    public Optional<String> findTokenInCache() {
        if (currentLeasedToken != null
            && currentLeasedTokenExpiryTimestamp > 0
            && currentLeasedTokenExpiryTimestamp > System.currentTimeMillis()) {
            return Optional.of(currentLeasedToken);
        }
        return Optional.empty();
    }
}
