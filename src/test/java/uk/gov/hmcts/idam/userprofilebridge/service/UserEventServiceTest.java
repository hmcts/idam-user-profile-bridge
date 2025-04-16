package uk.gov.hmcts.idam.userprofilebridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.cft.idam.api.v2.common.error.SpringWebClientHelper;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;
import uk.gov.hmcts.idam.userprofilebridge.properties.CategoryProperties;
import uk.gov.hmcts.idam.userprofilebridge.properties.IdamBridgeTargetProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEventServiceTest {

    @Mock
    UserProfileService userProfileService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CategoryProperties categoryProperties;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IdamBridgeTargetProperties idamBridgeTargetProperties;

    @InjectMocks
    UserEventService underTest;

    @BeforeEach
    public void setup() {
        when(categoryProperties.getRolePatterns().get("judiciary")).thenReturn(List.of("judiciary"));
        when(categoryProperties.getRolePatterns().get("caseworker")).thenReturn(List.of("caseworker"));
        when(categoryProperties.getRolePatterns().get("professional")).thenReturn(List.of("pui-.*"));
        when(categoryProperties.getRolePatterns().get("citizen")).thenReturn(List.of("citizen"));
        ReflectionTestUtils.setField(underTest, "caseworkerApiUpdatesEnabled", true);
        when(idamBridgeTargetProperties.getRd().getExcludedEventClientIds()).thenReturn(null);
    }

    @Test
    public void getUserProfileCategories() {
        User user = new User();
        user.setRoleNames(Arrays.asList("citizen", "caseworker", "judiciary", "pui-role", "other-role"));
        Set<UserProfileCategory> result = underTest.getUserProfileCategories(user);
        assertEquals(result.size(), 4);
        assertTrue(result.stream().anyMatch(s -> s == UserProfileCategory.CITIZEN));
        assertTrue(result.stream().anyMatch(s -> s == UserProfileCategory.CASEWORKER));
        assertTrue(result.stream().anyMatch(s -> s == UserProfileCategory.PROFESSIONAL));
        assertTrue(result.stream().anyMatch(s -> s == UserProfileCategory.JUDICIARY));
    }

    @Test
    public void getUserProfileCategories_noRoles() {
        User user = new User();
        Set<UserProfileCategory> result = underTest.getUserProfileCategories(user);
        assertEquals(result.size(), 1);
        assertTrue(result.stream().anyMatch(s -> s == UserProfileCategory.UNKNOWN));
    }

    @Test
    public void handleModifyUserEvent_citizen() {
        User user = new User();
        user.setRoleNames(List.of("citizen"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        underTest.handle(userEvent);
        verify(userProfileService, never()).syncIdamToUserProfile(any());
        verify(userProfileService, never()).syncIdamToCaseWorkerProfile(any());
    }

    @Test
    public void handleModifyUserEvent_userProfile() {
        User user = new User();
        user.setRoleNames(List.of("PUI-role"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        underTest.handle(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, never()).syncIdamToCaseWorkerProfile(any());
    }

    @Test
    public void handleModifyUserEvent_missingUserProfile() {
        User user = new User();
        user.setRoleNames(List.of("PUI-role"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        doThrow(SpringWebClientHelper.exception(HttpStatus.NOT_FOUND, new RuntimeException()))
            .when(userProfileService)
            .syncIdamToUserProfile(eq(userEvent.getUser()));
        underTest.handle(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, never()).syncIdamToCaseWorkerProfile(any());
    }

    @Test
    public void handleModifyUserEvent_userProfileException() {
        User user = new User();
        user.setRoleNames(List.of("PUI-role"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        doThrow(SpringWebClientHelper.exception(HttpStatus.I_AM_A_TEAPOT, new RuntimeException())).when(
            userProfileService).syncIdamToUserProfile(eq(userEvent.getUser()));
        try {
            underTest.handle(userEvent);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.I_AM_A_TEAPOT, hsce.getStatusCode());
        }
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, never()).syncIdamToCaseWorkerProfile(any());
    }

    @Test
    public void handleModifyUserEvent_caseworker() {
        User user = new User();
        user.setRoleNames(List.of("CASEWORKER"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        underTest.handle(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, times(1)).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
    }

    @Test
    public void handleModifyUserEvent_excludedClient() {
        User user = new User();
        user.setRoleNames(List.of("CASEWORKER"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        userEvent.setClientId("test-client");
        when(idamBridgeTargetProperties.getRd().getExcludedEventClientIds()).thenReturn(List.of("test-client"));
        underTest.handle(userEvent);
        verify(userProfileService, never()).syncIdamToUserProfile(any());
        verify(userProfileService, never()).syncIdamToCaseWorkerProfile(any());
    }

    @Test
    public void handleModifyUserEvent_missingCaseworker() {
        User user = new User();
        user.setRoleNames(List.of("CASEWORKER"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        doThrow(SpringWebClientHelper.exception(HttpStatus.NOT_FOUND, new RuntimeException()))
            .when(userProfileService)
            .syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
        underTest.handle(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, times(1)).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
    }

    @Test
    public void handleModifyUserEvent_caseworkerException() {
        User user = new User();
        user.setRoleNames(List.of("CASEWORKER"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        doThrow(SpringWebClientHelper.exception(HttpStatus.I_AM_A_TEAPOT, new RuntimeException())).when(
            userProfileService).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
        try {
            underTest.handle(userEvent);
            fail();
        } catch (HttpStatusCodeException hsce) {
            assertEquals(HttpStatus.I_AM_A_TEAPOT, hsce.getStatusCode());
        }
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, times(1)).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
    }

    @Test
    public void handleModifyUserEvent_caseworkerApiDisabled() {
        ReflectionTestUtils.setField(underTest, "caseworkerApiUpdatesEnabled", false);
        User user = new User();
        user.setRoleNames(List.of("CASEWORKER"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.MODIFY);
        underTest.handle(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, never()).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
    }

    @Test
    public void handleAddUserEvent_caseworker() {
        User user = new User();
        user.setRoleNames(List.of("caseworker"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.ADD);
        underTest.handle(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, times(1)).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
    }

    @Test
    public void excludeClient() {
        assertFalse(underTest.excludeClient("test-client", null));
        assertFalse(underTest.excludeClient("test-client", Collections.emptyList()));
        assertFalse(underTest.excludeClient(null, null));
        assertFalse(underTest.excludeClient(null, Collections.emptyList()));
        assertFalse(underTest.excludeClient("test-client", List.of("other-client")));
        assertTrue(underTest.excludeClient("test-client", List.of("test-client")));
    }

}
