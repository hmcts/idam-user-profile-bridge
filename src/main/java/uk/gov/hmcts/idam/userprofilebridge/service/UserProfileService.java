package uk.gov.hmcts.idam.userprofilebridge.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.cft.idam.api.v2.common.IdamV2UserManagementApi;
import uk.gov.hmcts.cft.idam.api.v2.common.model.AccountStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.RecordType;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.cft.rd.api.RDCaseWorkerApi;
import uk.gov.hmcts.cft.rd.api.RDUserProfileApi;
import uk.gov.hmcts.cft.rd.model.CaseWorkerProfile;
import uk.gov.hmcts.cft.rd.model.UserProfile;
import uk.gov.hmcts.cft.rd.model.UserStatus;

@Service
public class UserProfileService {

    private final IdamV2UserManagementApi idamV2UserManagementApi;

    private final RDUserProfileApi rdUserProfileApi;

    private final RDCaseWorkerApi rdCaseWorkerApi;

    public UserProfileService(IdamV2UserManagementApi idamV2UserManagementApi, RDUserProfileApi rdUserProfileApi,
                              RDCaseWorkerApi rdCaseWorkerApi) {
        this.idamV2UserManagementApi = idamV2UserManagementApi;
        this.rdUserProfileApi = rdUserProfileApi;
        this.rdCaseWorkerApi = rdCaseWorkerApi;
    }

    public User getUserById(String userId) {
        return idamV2UserManagementApi.getUser(userId);
    }

    public UserProfile getUserProfileById(String userId) {
        return rdUserProfileApi.getUserProfileById(userId);
    }

    public CaseWorkerProfile getCaseWorkerProfileById(String userId) {
        return rdCaseWorkerApi.findCaseWorkerProfileByUserId(userId);
    }

    public UserProfile syncIdamToUserProfile(String userId) {
        User idamUser = getUserById(userId);
        UserProfile userProfile = convertToUserProfileForDetailsUpdate(idamUser);
        rdUserProfileApi.updateUserProfile(userId, userProfile);
        return userProfile;
    }

    public CaseWorkerProfile syncIdamToCaseWorkerProfile(String userId) {
        User idamUser = getUserById(userId);
        CaseWorkerProfile caseWorkerProfile = convertToCaseWorkerProfileForDetailsUpdate(idamUser);
        rdCaseWorkerApi.updateCaseWorkerProfile(caseWorkerProfile);
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

    public UserProfile convertToUserProfileForDetailsUpdate(User user) {
        UserProfile userProfile = new UserProfile();
        userProfile.setEmail(user.getEmail());
        userProfile.setFirstName(user.getForename());
        userProfile.setLastName(user.getSurname());
        userProfile.setIdamStatus(convertToUserStatus(user.getAccountStatus(), user.getRecordType()));
        return userProfile;
    }

    public UserStatus convertToUserStatus(AccountStatus accountStatus, RecordType recordType) {
        if (recordType == RecordType.LIVE && accountStatus != AccountStatus.SUSPENDED) {
            return UserStatus.ACTIVE;
        }
        return UserStatus.SUSPENDED;
    }

    public boolean isCaseWorkerSuspended(AccountStatus accountStatus, RecordType recordType) {
        if (recordType == RecordType.LIVE && accountStatus != AccountStatus.SUSPENDED) {
            return false;
        }
        return true;
    }

}
