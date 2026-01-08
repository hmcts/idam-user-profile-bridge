package uk.gov.hmcts.idam.userprofilebridge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.IdamV2UserManagementApi;
import uk.gov.hmcts.cft.idam.api.v2.common.model.AccountStatus;
import uk.gov.hmcts.cft.idam.api.v2.common.model.RecordType;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.cft.rd.api.RefDataCaseWorkerApi;
import uk.gov.hmcts.cft.rd.api.RefDataJudicialUserApi;
import uk.gov.hmcts.cft.rd.api.RefDataUserProfileApi;
import uk.gov.hmcts.cft.rd.model.CaseWorkerProfile;
import uk.gov.hmcts.cft.rd.model.JudicialUserProfile;
import uk.gov.hmcts.cft.rd.model.UserProfile;
import uk.gov.hmcts.cft.rd.model.UserStatus;
import uk.gov.hmcts.idam.userprofilebridge.messaging.UserEventPublisher;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private IdamV2UserManagementApi idamV2UserManagementApi;

    @Mock
    private RefDataUserProfileApi refDataUserProfileApi;

    @Mock
    private RefDataCaseWorkerApi refDataCaseWorkerApi;

    @Mock
    private RefDataJudicialUserApi refDataJudicialUserApi;

    @Mock
    private UserEventPublisher userEventPublisher;

    @InjectMocks
    private UserProfileService underTest;

    @Test
    public void getUserById() {
        User testUser = new User();
        given(idamV2UserManagementApi.getUser("test-user-id")).willReturn(testUser);
        User result = underTest.getUserById("test-user-id");
        assertEquals(testUser, result);
    }

    @Test
    public void getUserProfileById() {
        UserProfile testUserProfile = new UserProfile();
        given(refDataUserProfileApi.getUserProfileById("test-user-id")).willReturn(testUserProfile);
        UserProfile result = underTest.getUserProfileById("test-user-id");
        assertEquals(testUserProfile, result);
    }

    @Test
    public void getCaseWorkerProfileById() {
        CaseWorkerProfile testCaseWorkerProfile = new CaseWorkerProfile();
        given(refDataCaseWorkerApi.findCaseWorkerProfileByUserId("test-user-id")).willReturn(testCaseWorkerProfile);
        CaseWorkerProfile result = underTest.getCaseWorkerProfileById("test-user-id");
        assertEquals(testCaseWorkerProfile, result);
    }

    @Test
    public void requestAddIdamUser() {
        User testUser = new User();
        given(idamV2UserManagementApi.getUser("test-user-id")).willReturn(testUser);
        underTest.requestAddIdamUser("test-user-id", "test-client-id");
        verify(userEventPublisher, times(1))
            .publish(testUser, EventType.ADD, "test-client-id");
    }

    @Test
    public void requestSyncIdamUser() {
        User testUser = new User();
        given(idamV2UserManagementApi.getUser("test-user-id")).willReturn(testUser);
        underTest.requestSyncIdamUser("test-user-id");
        verify(userEventPublisher, times(1))
            .publish(eq(testUser), eq(EventType.MODIFY), isNull());
    }

    @Test
    public void syncIdamToUserProfile_activeLive() {
        User testUser = getTestUser();
        UserProfile userProfile = new UserProfile();
        given(refDataUserProfileApi.getUserProfileById(testUser.getId())).willReturn(userProfile);
        UserProfile result = underTest.syncIdamToUserProfile(testUser);
        assertEquals(UserStatus.ACTIVE, result.getIdamStatus());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataUserProfileApi, times(1)).updateUserProfile(eq(testUser.getId()), any());
    }

    @Test
    public void syncIdamToUserProfile_archivedLive() {
        User testUser = getTestUser();
        testUser.setRecordType(RecordType.ARCHIVED);
        UserProfile userProfile = new UserProfile();
        given(refDataUserProfileApi.getUserProfileById(testUser.getId())).willReturn(userProfile);
        UserProfile result = underTest.syncIdamToUserProfile(testUser);
        assertEquals(UserStatus.SUSPENDED, result.getIdamStatus());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataUserProfileApi, times(1)).updateUserProfile(eq(testUser.getId()), any());
    }

    @Test
    public void syncIdamToUserProfile_activeSuspended() {
        User testUser = getTestUser();
        testUser.setAccountStatus(AccountStatus.SUSPENDED);
        UserProfile userProfile = new UserProfile();
        given(refDataUserProfileApi.getUserProfileById(testUser.getId())).willReturn(userProfile);
        UserProfile result = underTest.syncIdamToUserProfile(testUser);
        assertEquals(UserStatus.SUSPENDED, result.getIdamStatus());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataUserProfileApi, times(1)).updateUserProfile(eq(testUser.getId()), any());
    }

    @Test
    public void syncIdamToUserProfile_notFound() {
        User testUser = getTestUser();
        given(refDataUserProfileApi.getUserProfileById(testUser.getId())).willThrow(new HttpClientErrorException(
            HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value())));
        try {
            underTest.syncIdamToUserProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.NOT_FOUND, hsce.getStatusCode());
        }
    }

    @Test
    public void syncIdamToUserProfile_notFoundInconsistentEmailIdentity() {
        User testUser = getTestUser();
        given(refDataUserProfileApi.getUserProfileById(testUser.getId())).willThrow(new HttpClientErrorException(
            HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value())));
        UserProfile userProfile = new UserProfile();
        userProfile.setIdamId("inconsistent-id");
        given(refDataUserProfileApi.getUserProfileByEmail(testUser.getEmail())).willReturn(userProfile);
        try {
            underTest.syncIdamToUserProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.NOT_FOUND, hsce.getStatusCode());
        }
    }

    @Test
    public void syncIdamToCaseWorkerProfile_activeLive() {
        User testUser = getTestUser();
        CaseWorkerProfile caseWorkerProfile = new CaseWorkerProfile();
        given(refDataCaseWorkerApi.findCaseWorkerProfileByUserId(testUser.getId())).willReturn(caseWorkerProfile);
        CaseWorkerProfile result = underTest.syncIdamToCaseWorkerProfile(testUser);
        assertFalse(result.isSuspended());
        assertEquals(testUser.getId(), result.getCaseWorkerId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataCaseWorkerApi, times(1)).updateCaseWorkerProfile(any());
    }

    @Test
    public void syncIdamToCaseWorkerProfile_archivedLive() {
        User testUser = getTestUser();
        testUser.setRecordType(RecordType.ARCHIVED);
        CaseWorkerProfile caseWorkerProfile = new CaseWorkerProfile();
        given(refDataCaseWorkerApi.findCaseWorkerProfileByUserId(testUser.getId())).willReturn(caseWorkerProfile);
        CaseWorkerProfile result = underTest.syncIdamToCaseWorkerProfile(testUser);
        assertTrue(result.isSuspended());
        assertEquals(testUser.getId(), result.getCaseWorkerId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataCaseWorkerApi, times(1)).updateCaseWorkerProfile(any());
    }

    @Test
    public void syncIdamToCaseWorkerProfile_activeSuspended() {
        User testUser = getTestUser();
        testUser.setRecordType(RecordType.ARCHIVED);
        CaseWorkerProfile caseWorkerProfile = new CaseWorkerProfile();
        given(refDataCaseWorkerApi.findCaseWorkerProfileByUserId(testUser.getId())).willReturn(caseWorkerProfile);
        CaseWorkerProfile result = underTest.syncIdamToCaseWorkerProfile(testUser);
        assertTrue(result.isSuspended());
        assertEquals(testUser.getId(), result.getCaseWorkerId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataCaseWorkerApi, times(1)).updateCaseWorkerProfile(any());
    }

    @Test
    public void syncIdamToCaseWorkerProfile_notFound() {
        User testUser = getTestUser();
        given(refDataCaseWorkerApi.findCaseWorkerProfileByUserId(testUser.getId()))
            .willThrow(new HttpClientErrorException(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value())));
        try {
            underTest.syncIdamToCaseWorkerProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.NOT_FOUND, hsce.getStatusCode());
        }
    }

    @Test
    public void getJudicialUserProfileByIdamId_success() {
        JudicialUserProfile testJudicialProfile = new JudicialUserProfile();
        given(refDataJudicialUserApi.getAllUsersByIdamId("test-idam-id")).willReturn(List.of(testJudicialProfile));
        JudicialUserProfile result = underTest.getJudicialUserProfileByIdamId("test-idam-id");
        assertEquals(testJudicialProfile, result);
    }

    @Test
    public void getJudicialUserProfileByIdamId_notFound() {
        given(refDataJudicialUserApi.getAllUsersByIdamId("test-idam-id")).willReturn(Collections.emptyList());
        try {
            underTest.getJudicialUserProfileByIdamId("test-idam-id");
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.NOT_FOUND, hsce.getStatusCode());
        }
    }

    @Test
    public void getJudicialUserProfileByIdamId_conflict() {
        JudicialUserProfile profile1 = new JudicialUserProfile();
        profile1.setPersonalCode("test-personal-code-1");
        JudicialUserProfile profile2 = new JudicialUserProfile();
        profile2.setPersonalCode("test-personal-code-2");
        given(refDataJudicialUserApi.getAllUsersByIdamId("test-idam-id")).willReturn(List.of(profile1, profile2));
        try {
            underTest.getJudicialUserProfileByIdamId("test-idam-id");
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.CONFLICT, hsce.getStatusCode());
        }
    }

    @Test
    public void getJudicialUserProfileBySsoId_success() {
        JudicialUserProfile testJudicialProfile = new JudicialUserProfile();
        given(refDataJudicialUserApi.getAllUsersByObjectId("test-sso-id")).willReturn(List.of(testJudicialProfile));
        JudicialUserProfile result = underTest.getJudicialUserProfileBySsoId("test-sso-id");
        assertEquals(testJudicialProfile, result);
    }

    @Test
    public void getJudicialUserProfileBySsoId_notFound() {
        given(refDataJudicialUserApi.getAllUsersByObjectId("test-sso-id")).willReturn(Collections.emptyList());
        try {
            underTest.getJudicialUserProfileBySsoId("test-sso-id");
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.NOT_FOUND, hsce.getStatusCode());
        }
    }

    @Test
    public void getJudicialUserProfileBySsoId_conflict() {
        JudicialUserProfile profile1 = new JudicialUserProfile();
        profile1.setPersonalCode("test-personal-code-1");
        JudicialUserProfile profile2 = new JudicialUserProfile();
        profile2.setPersonalCode("test-personal-code-2");
        given(refDataJudicialUserApi.getAllUsersByObjectId("test-sso-id")).willReturn(List.of(profile1, profile2));
        try {
            underTest.getJudicialUserProfileBySsoId("test-sso-id");
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.CONFLICT, hsce.getStatusCode());
        }
    }

    @Test
    public void validateIdamToJudicialUserProfile_success() {
        User testUser = getTestUser();
        JudicialUserProfile testJudicialProfile = new JudicialUserProfile();
        testJudicialProfile.setSidamId(testUser.getId());
        testJudicialProfile.setObjectId(testUser.getSsoId());
        testJudicialProfile.setEmail(testUser.getEmail());
        given(refDataJudicialUserApi.getAllUsersByObjectId("test-sso-id")).willReturn(List.of(testJudicialProfile));
        JudicialUserProfile result = underTest.validateIdamToJudicialUserProfile(testUser);
        assertEquals(testJudicialProfile, result);
        verify(refDataJudicialUserApi, never()).getAllUsersByIdamId(any());
    }

    @Test
    public void validateIdamToJudicialUserProfile_notFound() {
        User testUser = getTestUser();
        given(refDataJudicialUserApi.getAllUsersByObjectId(testUser.getSsoId())).willReturn(Collections.emptyList());
        given(refDataJudicialUserApi.getAllUsersByIdamId(testUser.getId())).willReturn(Collections.emptyList());
        try {
            underTest.validateIdamToJudicialUserProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.NOT_FOUND, hsce.getStatusCode());
        }
    }

    @Test
    public void validateIdamToJudicialUserProfile_conflictByEmail() {
        User testUser = getTestUser();
        JudicialUserProfile testJudicialProfile = new JudicialUserProfile();
        testJudicialProfile.setSidamId(testUser.getId());
        testJudicialProfile.setObjectId(testUser.getSsoId());
        testJudicialProfile.setEmail("different-test-email");
        given(refDataJudicialUserApi.getAllUsersByObjectId("test-sso-id")).willReturn(List.of(testJudicialProfile));
        try {
            underTest.validateIdamToJudicialUserProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.CONFLICT, hsce.getStatusCode());
        }
        verify(refDataJudicialUserApi, never()).getAllUsersByIdamId(any());
    }

    @Test
    public void validateIdamToJudicialUserProfile_conflictBySsoId() {
        User testUser = getTestUser();
        JudicialUserProfile testJudicialProfile = new JudicialUserProfile();
        testJudicialProfile.setSidamId(testUser.getId());
        testJudicialProfile.setObjectId("different-sso-id");
        testJudicialProfile.setEmail(testUser.getEmail());
        given(refDataJudicialUserApi.getAllUsersByObjectId("test-sso-id")).willReturn(Collections.emptyList());
        given(refDataJudicialUserApi.getAllUsersByIdamId(testUser.getId())).willReturn(List.of(testJudicialProfile));
        try {
            underTest.validateIdamToJudicialUserProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.CONFLICT, hsce.getStatusCode());
        }
    }

    @Test
    public void validateIdamToJudicialUserProfile_conflictByIdamId() {
        User testUser = getTestUser();
        JudicialUserProfile testJudicialProfile = new JudicialUserProfile();
        testJudicialProfile.setSidamId("different-idam-id");
        testJudicialProfile.setObjectId(testUser.getSsoId());
        testJudicialProfile.setEmail(testUser.getEmail());
        given(refDataJudicialUserApi.getAllUsersByObjectId("test-sso-id")).willReturn(List.of(testJudicialProfile));
        try {
            underTest.validateIdamToJudicialUserProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.CONFLICT, hsce.getStatusCode());
        }
        verify(refDataJudicialUserApi, never()).getAllUsersByIdamId(any());
    }

    @Test
    public void validateIdamToJudicialUserProfile_conflictByMissingSsoId() {
        User testUser = getTestUser();
        testUser.setSsoId(null);
        JudicialUserProfile testJudicialProfile = new JudicialUserProfile();
        testJudicialProfile.setSidamId(testUser.getId());
        testJudicialProfile.setObjectId("test-sso-id");
        testJudicialProfile.setEmail(testUser.getEmail());
        given(refDataJudicialUserApi.getAllUsersByIdamId(testUser.getId())).willReturn(List.of(testJudicialProfile));
        try {
            underTest.validateIdamToJudicialUserProfile(testUser);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.CONFLICT, hsce.getStatusCode());
        }
        verify(refDataJudicialUserApi, never()).getAllUsersByObjectId(any());
    }

    private User getTestUser() {
        User testUser = new User();
        testUser.setEmail("test-email");
        testUser.setId("test-user-id");
        testUser.setSsoId("test-sso-id");
        testUser.setAccountStatus(AccountStatus.ACTIVE);
        testUser.setRecordType(RecordType.LIVE);
        return testUser;
    }

}
