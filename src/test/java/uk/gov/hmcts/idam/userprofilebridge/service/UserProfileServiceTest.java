package uk.gov.hmcts.idam.userprofilebridge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
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
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.idam.userprofilebridge.messaging.UserEventListener.MODIFY_USER_DESTINATION;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private IdamV2UserManagementApi idamV2UserManagementApi;

    @Mock
    private RefDataUserProfileApi refDataUserProfileApi;

    @Mock
    private RefDataCaseWorkerApi refDataCaseWorkerApi;

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
        verify(userEventPublisher, times(1)).publish(testUser, EventType.ADD, "test-client-id");
    }

    @Test
    public void requestSyncIdamUser() {
        User testUser = new User();
        given(idamV2UserManagementApi.getUser("test-user-id")).willReturn(testUser);
        underTest.requestSyncIdamUser("test-user-id");
        verify(userEventPublisher, times(1)).publish(eq(testUser), eq(EventType.MODIFY), isNull());
    }

    @Test
    public void syncIdamToUserProfile_activeLive() {
        User testUser = getTestUser();
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
        UserProfile result = underTest.syncIdamToUserProfile(testUser);
        assertEquals(UserStatus.SUSPENDED, result.getIdamStatus());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataUserProfileApi, times(1)).updateUserProfile(eq(testUser.getId()), any());
    }

    @Test
    public void syncIdamToCaseWorkerProfile_activeLive() {
        User testUser = getTestUser();
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
        CaseWorkerProfile result = underTest.syncIdamToCaseWorkerProfile(testUser);
        assertTrue(result.isSuspended());
        assertEquals(testUser.getId(), result.getCaseWorkerId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getForename(), result.getFirstName());
        assertEquals(testUser.getSurname(), result.getLastName());
        verify(refDataCaseWorkerApi, times(1)).updateCaseWorkerProfile(any());
    }

    private User getTestUser() {
        User testUser = new User();
        testUser.setId("test-user-id");
        testUser.setAccountStatus(AccountStatus.ACTIVE);
        testUser.setRecordType(RecordType.LIVE);
        return testUser;
    }

}
