package uk.gov.hmcts.idam.userprofilebridge;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.cft.idam.api.v2.common.IdamV2UserManagementApi;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;

@Service
public class UserProfileService {

    private final IdamV2UserManagementApi idamV2UserManagementApi;

    public UserProfileService(IdamV2UserManagementApi idamV2UserManagementApi) {
        this.idamV2UserManagementApi = idamV2UserManagementApi;
    }

    public User getUserById(String userId) {
        return idamV2UserManagementApi.getUser(userId);
    }

}
