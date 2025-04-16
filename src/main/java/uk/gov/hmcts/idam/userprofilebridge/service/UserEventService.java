package uk.gov.hmcts.idam.userprofilebridge.service;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;
import uk.gov.hmcts.idam.userprofilebridge.properties.CategoryProperties;
import uk.gov.hmcts.idam.userprofilebridge.properties.IdamBridgeProperties;
import uk.gov.hmcts.idam.userprofilebridge.trace.TraceAttribute;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.CASEWORKER;
import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.CITIZEN;
import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.JUDICIARY;
import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.PROFESSIONAL;

@Slf4j
@Service
public class UserEventService {

    public static final EnumSet<UserProfileCategory> UP_SYSTEM_CATEGORIES = EnumSet.of(PROFESSIONAL, CASEWORKER);
    private final UserProfileService userProfileService;
    private final CategoryProperties categoryProperties;
    private final IdamBridgeProperties idamBridgeProperties;

    @Value("${rd.caseworker.api.enabled:true}")
    private boolean caseworkerApiUpdatesEnabled;

    public UserEventService(UserProfileService userProfileService, CategoryProperties categoryProperties,
                            IdamBridgeProperties idamBridgeProperties) {
        this.userProfileService = userProfileService;
        this.categoryProperties = categoryProperties;
        this.idamBridgeProperties = idamBridgeProperties;
    }

    public void handle(UserEvent userEvent) {
        Set<UserProfileCategory> userProfileCategories = getUserProfileCategories(userEvent.getUser());
        Span.current().setAttribute(TraceAttribute.CATEGORIES,
                                    userProfileCategories.stream().map(Enum::name).collect(Collectors.joining(","))
        );
        if (excludeClient(userEvent.getClientId(), idamBridgeProperties.getExcludedClients())) {
            Span.current().setAttribute(TraceAttribute.EXCLUDED_CLIENT, userEvent.getClientId());
        } else {
            modifyRefDataProfiles(userEvent, userProfileCategories);
        }
    }

    protected boolean excludeClient(String clientId, List<String> excludedClients) {
        if (StringUtils.isNotEmpty(clientId) && CollectionUtils.isNotEmpty(excludedClients)) {
            return excludedClients.stream().anyMatch(ec -> ec.equalsIgnoreCase(clientId));
        }
        return false;
    }

    private void modifyRefDataProfiles(UserEvent userEvent, Set<UserProfileCategory> userProfileCategories) {
        if (CollectionUtils.containsAny(userProfileCategories, UP_SYSTEM_CATEGORIES)) {
            try {
                userProfileService.syncIdamToUserProfile(userEvent.getUser());
                Span.current().setAttribute(TraceAttribute.USER_PROFILE_STATE, SyncState.OKAY.name());
            } catch (HttpStatusCodeException hsce) {
                if (hsce.getStatusCode() == HttpStatus.NOT_FOUND) {
                    Span.current().setAttribute(TraceAttribute.USER_PROFILE_STATE, SyncState.NOT_FOUND.name());
                } else {
                    throw hsce;
                }
            }
        }
        if (userProfileCategories.contains(CASEWORKER) && caseworkerApiUpdatesEnabled) {
            try {
                userProfileService.syncIdamToCaseWorkerProfile(userEvent.getUser());
                Span.current().setAttribute(TraceAttribute.CASEWORKER_PROFILE_STATE, SyncState.OKAY.name());
            } catch (HttpStatusCodeException hsce) {
                if (hsce.getStatusCode() == HttpStatus.NOT_FOUND) {
                    Span.current().setAttribute(TraceAttribute.CASEWORKER_PROFILE_STATE, SyncState.NOT_FOUND.name());
                } else {
                    throw hsce;
                }
            }
        }
    }

    protected Set<UserProfileCategory> getUserProfileCategories(User user) {
        return getUserProfileCategories(user.getRoleNames());
    }

    protected Set<UserProfileCategory> getUserProfileCategories(List<String> roleNames) {
        Set<UserProfileCategory> categories = EnumSet.noneOf(UserProfileCategory.class);
        if (CollectionUtils.isNotEmpty(roleNames)) {
            for (String roleName : roleNames) {
                if (matchesAny(roleName, categoryProperties.getRolePatterns().get("judiciary"))) {
                    categories.add(JUDICIARY);
                } else if (matchesAny(roleName, categoryProperties.getRolePatterns().get("citizen"))) {
                    categories.add(CITIZEN);
                } else if (matchesAny(roleName, categoryProperties.getRolePatterns().get("professional"))) {
                    categories.add(PROFESSIONAL);
                } else if (matchesAny(roleName, categoryProperties.getRolePatterns().get("caseworker"))) {
                    categories.add(CASEWORKER);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(categories)) {
            return categories;
        }
        return Collections.singleton(UserProfileCategory.UNKNOWN);
    }

    protected boolean matchesAny(String value, List<String> patterns) {
        return patterns.stream().anyMatch(value.toLowerCase()::matches);
    }

    private enum SyncState {
        OKAY, NOT_FOUND
    }

}
