package uk.gov.hmcts.idam.userprofilebridge.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.CASEWORKER;
import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.CITIZEN;
import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.JUDICIARY;
import static uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory.PROFESSIONAL;

@Slf4j
@Service
public class UserEventService {

    @Value("${cft.categories.judiciary.role-patterns}")
    private List<String> judicaryRoleRegexList;

    @Value("${cft.categories.caseworker.role-patterns}")
    private List<String> caseworkerRoleRegexList;

    @Value("${cft.categories.professional.role-patterns}")
    private List<String> professionalRoleRegexList;

    @Value("${cft.categories.citizen.role-patterns}")
    private List<String> citizenRoleRegexList;

    private final UserProfileService userProfileService;

    public static final EnumSet<UserProfileCategory> UP_SYSTEM_CATEGORIES = EnumSet.of(
        PROFESSIONAL,
        CASEWORKER,
        JUDICIARY
    );

    public UserEventService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    public void handleAddUserEvent(UserEvent userEvent) {
        Set<UserProfileCategory> userProfileCategories = getUserProfileCategories(userEvent.getUser());
        log.info(
            "Received add user event for id {}, for categories {}",
            userEvent.getUser().getId(),
            userProfileCategories
        );
        modifyRefDataProfiles(userEvent, userProfileCategories);
    }

    public void handleModifyUserEvent(UserEvent userEvent) {
        Set<UserProfileCategory> userProfileCategories = getUserProfileCategories(userEvent.getUser());
        log.info(
            "Received modify user event for id {}, for categories {}",
            userEvent.getUser().getId(),
            userProfileCategories
        );
        modifyRefDataProfiles(userEvent, userProfileCategories);
    }

    private void modifyRefDataProfiles(UserEvent userEvent, Set<UserProfileCategory> userProfileCategories) {
        if (CollectionUtils.containsAny(userProfileCategories, UP_SYSTEM_CATEGORIES)) {
            userProfileService.syncIdamToUserProfile(userEvent.getUser());
        }
        if (userProfileCategories.contains(CASEWORKER)) {
            userProfileService.syncIdamToCaseWorkerProfile(userEvent.getUser());
        }
    }

    protected Set<UserProfileCategory> getUserProfileCategories(User user) {
        return getUserProfileCategories(user.getRoleNames());
    }

    protected Set<UserProfileCategory> getUserProfileCategories(List<String> roleNames) {
        Set<UserProfileCategory> categories = new HashSet<>();
        if (CollectionUtils.isNotEmpty(roleNames)) {
            for (String roleName : roleNames) {
                if (matchesAny(roleName, judicaryRoleRegexList)) {
                    categories.add(JUDICIARY);
                } else if (matchesAny(roleName, citizenRoleRegexList)) {
                    categories.add(CITIZEN);
                } else if (matchesAny(roleName, professionalRoleRegexList)) {
                    categories.add(PROFESSIONAL);
                } else if (matchesAny(roleName, caseworkerRoleRegexList)) {
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

}
