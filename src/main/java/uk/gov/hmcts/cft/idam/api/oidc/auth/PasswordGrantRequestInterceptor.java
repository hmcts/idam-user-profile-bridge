package uk.gov.hmcts.cft.idam.api.oidc.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.regex.Pattern;

import static java.util.Objects.isNull;

public class PasswordGrantRequestInterceptor implements RequestInterceptor {

    private static final String AUTH_HEADER = "Authorization";

    private static final String BEARER = "Bearer";

    private final ClientRegistration clientRegistration;

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    private final String resourceOwnerUsername;

    private final String resourceOwnerPassword;

    private final Pattern matchesPattern;

    private final Authentication principal;

    public PasswordGrantRequestInterceptor(ClientRegistration clientRegistration, OAuth2AuthorizedClientManager authorizedClientManager,
                                           String resourceOwnerUsername, String resourceOwnerPassword, String matchesRegex) {
        this.clientRegistration = clientRegistration;
        this.authorizedClientManager = authorizedClientManager;
        this.principal = new ClientPrincipal(clientRegistration.getClientId());
        this.resourceOwnerUsername = resourceOwnerUsername;
        this.resourceOwnerPassword = resourceOwnerPassword;
        this.matchesPattern = Pattern.compile(matchesRegex);
    }

    @Override
    public void apply(RequestTemplate template) {
        if (handleUrl(template.url())) {
            addBearer(template, getAccessToken());
        }
    }

    private boolean handleUrl(String url) {
        return url != null && matchesPattern.matcher(url).find();
    }

    private void addBearer(RequestTemplate template, String token) {
        template.header(AUTH_HEADER, BEARER + " " + token);
    }

    private String getAccessToken() {
        OAuth2AuthorizedClient client = authorizedClientManager
            .authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistration.getRegistrationId())
                    .principal(principal)
                    .attributes(attrs -> {
                        attrs.put(OAuth2ParameterNames.USERNAME, resourceOwnerUsername);
                        attrs.put(OAuth2ParameterNames.PASSWORD, resourceOwnerPassword);
                    })
                    .build());
        if (isNull(client)) {
            throw new IllegalStateException("password grant flow on " + clientRegistration
                .getRegistrationId() + " failed, client is null");
        }
        return client.getAccessToken().getTokenValue();
    }

}
