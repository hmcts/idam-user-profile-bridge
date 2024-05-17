package uk.gov.hmcts.idam.userprofilebridge.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.IdamV2UserManagementApi;
import uk.gov.hmcts.cft.idam.api.v2.common.model.AccountStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.RecordType;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.cft.idam.api.v2.common.util.LogUtil;
import uk.gov.hmcts.cft.rd.api.RefDataCaseWorkerApi;
import uk.gov.hmcts.cft.rd.api.RefDataUserProfileApi;
import uk.gov.hmcts.cft.rd.model.CaseWorkerProfile;
import uk.gov.hmcts.cft.rd.model.UserProfile;
import uk.gov.hmcts.cft.rd.model.UserStatus;
import uk.gov.hmcts.idam.userprofilebridge.messaging.UserEventPublisher;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;

import java.util.Optional;

import static uk.gov.hmcts.cft.idam.api.v2.common.util.ResponseUtil.asOptional;

@Service
@Slf4j
public class UserProfileService {

    private static final int EMAIL_VISIBLE = 7;
    private final IdamV2UserManagementApi idamV2UserManagementApi;

    private final RefDataUserProfileApi refDataUserProfileApi;

    private final RefDataCaseWorkerApi refDataCaseWorkerApi;

    private final UserEventPublisher userEventPublisher;

    public UserProfileService(IdamV2UserManagementApi idamV2UserManagementApi,
                              RefDataUserProfileApi refDataUserProfileApi,
                              RefDataCaseWorkerApi refDataCaseWorkerApi,
                              UserEventPublisher userEventPublisher) {
        this.idamV2UserManagementApi = idamV2UserManagementApi;
        this.refDataUserProfileApi = refDataUserProfileApi;
        this.refDataCaseWorkerApi = refDataCaseWorkerApi;
        this.userEventPublisher = userEventPublisher;
    }

    public User getUserById(String userId) {
        return idamV2UserManagementApi.getUser(userId);
    }

    public UserProfile getUserProfileById(String userId) {
        return refDataUserProfileApi.getUserProfileById(userId);
    }

    public UserProfile getUserProfileForUpdate(String userId, String email) {
        try {
            return getUserProfileById(userId);
        } catch (HttpStatusCodeException hsce) {
            if (hsce.getStatusCode() == HttpStatus.NOT_FOUND) {
                Optional<UserProfile> existingUserProfile =
                    asOptional(() -> refDataUserProfileApi.getUserProfileByEmail(email));
                existingUserProfile.ifPresent(userProfile -> log.warn(
                    "Inconsistent identity for email '{}', user id '{}', user-profile id '{}'",
                    LogUtil.obfuscateEmail(email, EMAIL_VISIBLE),
                    userId,
                    StringUtils.firstNonEmpty(
                        existingUserProfile.get().getIdamId(), existingUserProfile.get().getUserIdentifier(), "n/a")
                ));
            }
            throw hsce;
        }
    }

    public CaseWorkerProfile getCaseWorkerProfileById(String userId) {
        return refDataCaseWorkerApi.findCaseWorkerProfileByUserId(userId);
    }

    public void requestAddIdamUser(String userId, String clientId) {
        User user = getUserById(userId);
        log.info("Publishing add user event for id {} and client {}", user.getId(), clientId);
        userEventPublisher.publish(user, EventType.ADD, clientId);
    }

    public void requestSyncIdamUser(String userId) {
        requestSyncIdamUser(userId, null);
    }

    public void requestSyncIdamUser(String userId, String clientId) {
        User user = getUserById(userId);
        userEventPublisher.publish(user, EventType.MODIFY, clientId);
    }

    public UserProfile syncIdamToUserProfile(User idamUser) {
        UserProfile existingUserProfile = getUserProfileForUpdate(idamUser.getId(), idamUser.getEmail());
        compareDetails(idamUser, existingUserProfile);
        UserProfile userProfile = convertToUserProfileForDetailsUpdate(idamUser);
        refDataUserProfileApi.updateUserProfile(idamUser.getId(), userProfile);
        return userProfile;
    }

    public CaseWorkerProfile syncIdamToCaseWorkerProfile(User idamUser) {
        CaseWorkerProfile existingCaseWorkerProfile = getCaseWorkerProfileById(idamUser.getId());
        compareDetails(idamUser, existingCaseWorkerProfile);
        CaseWorkerProfile caseWorkerProfile = convertToCaseWorkerProfileForDetailsUpdate(idamUser);
        refDataCaseWorkerApi.updateCaseWorkerProfile(caseWorkerProfile);
        return caseWorkerProfile;
    }

    private void compareDetails(User idamUser, UserProfile existingUserProfile) {
        if (!StringUtils.equalsIgnoreCase(idamUser.getEmail(), existingUserProfile.getEmail())) {
            log.info("Email changed for user id '{}', user-profile email '{}' will be replaced",
                     idamUser.getId(),
                     LogUtil.obfuscateEmail(existingUserProfile.getEmail(), EMAIL_VISIBLE)
            );
        }
        if (existingUserProfile.getIdamStatus() != convertToUserStatus(idamUser.getAccountStatus(), idamUser.getRecordType())) {
            log.info(
                "user-profile status change from '{}' to match idam account status '{}' and record type '{}'",
                existingUserProfile.getIdamStatus(),
                idamUser.getAccountStatus(),
                idamUser.getRecordType());
        }
    }

    private void compareDetails(User idamUser, CaseWorkerProfile existingCaseWorkerProfile) {
        if (!StringUtils.equalsIgnoreCase(idamUser.getEmail(), existingCaseWorkerProfile.getEmail())) {
            log.info("Email changed for user id {}, caseworker profile email {} will be replaced",
                     idamUser.getId(),
                     LogUtil.obfuscateEmail(existingCaseWorkerProfile.getEmail(), EMAIL_VISIBLE)
            );
        }
        if (existingCaseWorkerProfile.isSuspended() != isCaseWorkerSuspended(idamUser.getAccountStatus(), idamUser.getRecordType())) {
            log.info(
                "caseworker status change from '{}' to match idam account status '{}' and record type '{}'",
                existingCaseWorkerProfile.isSuspended() ? "suspended" : "active",
                idamUser.getAccountStatus(),
                idamUser.getRecordType());
        }
    }

    private CaseWorkerProfile convertToCaseWorkerProfileForDetailsUpdate(User user) {
        CaseWorkerProfile caseWorkerProfile = new CaseWorkerProfile();
        caseWorkerProfile.setCaseWorkerId(user.getId());
        caseWorkerProfile.setEmail(user.getEmail());
        caseWorkerProfile.setFirstName(user.getForename());
        caseWorkerProfile.setLastName(user.getSurname());
        caseWorkerProfile.setSuspended(isCaseWorkerSuspended(user.getAccountStatus(), user.getRecordType()));
        return caseWorkerProfile;
    }

    private UserProfile convertToUserProfileForDetailsUpdate(User user) {
        UserProfile userProfile = new UserProfile();
        userProfile.setEmail(user.getEmail());
        userProfile.setFirstName(user.getForename());
        userProfile.setLastName(user.getSurname());
        userProfile.setIdamStatus(convertToUserStatus(user.getAccountStatus(), user.getRecordType()));
        return userProfile;
    }

    private UserStatus convertToUserStatus(AccountStatus accountStatus, RecordType recordType) {
        if (recordType == RecordType.LIVE && accountStatus != AccountStatus.SUSPENDED) {
            return UserStatus.ACTIVE;
        }
        return UserStatus.SUSPENDED;
    }

    private boolean isCaseWorkerSuspended(AccountStatus accountStatus, RecordType recordType) {
        return recordType != RecordType.LIVE || accountStatus == AccountStatus.SUSPENDED;
    }

}
