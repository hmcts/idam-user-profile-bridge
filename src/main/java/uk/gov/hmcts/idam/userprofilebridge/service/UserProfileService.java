package uk.gov.hmcts.idam.userprofilebridge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cft.idam.api.v2.common.IdamV2UserManagementApi;
import uk.gov.hmcts.cft.idam.api.v2.common.model.AccountStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.RecordType;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.cft.rd.api.RefDataCaseWorkerApi;
import uk.gov.hmcts.cft.rd.api.RefDataUserProfileApi;
import uk.gov.hmcts.cft.rd.model.CaseWorkerProfile;
import uk.gov.hmcts.cft.rd.model.UserProfile;
import uk.gov.hmcts.cft.rd.model.UserStatus;
import uk.gov.hmcts.idam.userprofilebridge.messaging.UserEventPublisher;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;

@Service
@Slf4j
public class UserProfileService {

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
        UserProfile userProfile = convertToUserProfileForDetailsUpdate(idamUser);
        refDataUserProfileApi.updateUserProfile(idamUser.getId(), userProfile);
        return userProfile;
    }

    public CaseWorkerProfile syncIdamToCaseWorkerProfile(User idamUser) {
        CaseWorkerProfile caseWorkerProfile = convertToCaseWorkerProfileForDetailsUpdate(idamUser);
        refDataCaseWorkerApi.updateCaseWorkerProfile(caseWorkerProfile);
        return caseWorkerProfile;
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
