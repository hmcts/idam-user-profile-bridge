package uk.gov.hmcts.idam.userprofilebridge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class UserEventServiceTest {

    @Mock
    UserProfileService userProfileService;

    @InjectMocks
    UserEventService underTest;

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

}
