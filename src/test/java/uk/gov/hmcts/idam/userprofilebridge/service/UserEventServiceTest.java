package uk.gov.hmcts.idam.userprofilebridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.EventType;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventServiceTest {

    @Mock
    UserProfileService userProfileService;

    @InjectMocks
    UserEventService underTest;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "judicaryRoleRegexList", List.of("judiciary"));
        ReflectionTestUtils.setField(underTest, "caseworkerRoleRegexList", List.of("caseworker"));
        ReflectionTestUtils.setField(underTest, "professionalRoleRegexList", List.of("pui.*"));
        ReflectionTestUtils.setField(underTest, "citizenRoleRegexList", List.of("citizen"));
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
        underTest.handleModifyUserEvent(userEvent);
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
        underTest.handleModifyUserEvent(userEvent);
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
        underTest.handleModifyUserEvent(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, times(1)).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
    }

    @Test
    public void handleAddUserEvent_caseworker() {
        User user = new User();
        user.setRoleNames(List.of("caseworker"));
        UserEvent userEvent = new UserEvent();
        userEvent.setUser(user);
        userEvent.setEventType(EventType.ADD);
        underTest.handleAddUserEvent(userEvent);
        verify(userProfileService, times(1)).syncIdamToUserProfile(eq(userEvent.getUser()));
        verify(userProfileService, times(1)).syncIdamToCaseWorkerProfile(eq(userEvent.getUser()));
    }

}
