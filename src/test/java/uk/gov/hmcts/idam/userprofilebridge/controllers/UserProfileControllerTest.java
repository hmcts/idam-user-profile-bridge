package uk.gov.hmcts.idam.userprofilebridge.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.idam.userprofilebridge.config.SecurityConfig;
import uk.gov.hmcts.idam.userprofilebridge.service.UserProfileService;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ImportAutoConfiguration(classes = {SecurityConfig.class})
class UserProfileControllerTest {

    private final static String VIEW_USER_PROFILE_SCOPE = "SCOPE_view-user-profile";
    private final static String SYNC_USER_PROFILE_SCOPE = "SCOPE_sync-user-profile";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    UserProfileService userProfileService;

    @Test
    public void getUserById() throws Exception {
        mockMvc.perform(
                get("/idam/api/v2/users/1234")
                    .with(jwt()
                              .authorities(new SimpleGrantedAuthority(VIEW_USER_PROFILE_SCOPE))
                              .jwt(token -> token.claim("aud", "test-client").build())))
            .andExpect(status().isOk());

        verify(userProfileService).getUserById("1234");
    }

    @Test
    public void getUserProfileById() throws Exception {
        mockMvc.perform(
                get("/rd/api/v1/userprofile/1234")
                    .with(jwt()
                              .authorities(new SimpleGrantedAuthority(VIEW_USER_PROFILE_SCOPE))
                              .jwt(token -> token.claim("aud", "test-client").build())))
            .andExpect(status().isOk());

        verify(userProfileService).getUserProfileById("1234");
    }

    @Test
    public void getCaseworkerProfileById() throws Exception {
        mockMvc.perform(
                get("/rd/case-worker/profile/search-by-id/1234")
                    .with(jwt()
                              .authorities(new SimpleGrantedAuthority(VIEW_USER_PROFILE_SCOPE))
                              .jwt(token -> token.claim("aud", "test-client").build())))
            .andExpect(status().isOk());

        verify(userProfileService).getCaseWorkerProfileById("1234");
    }

    @Test
    public void syncIdamUser() throws Exception {
        mockMvc.perform(
            put("/bridge/user/1234")
                .with(jwt()
                          .authorities(new SimpleGrantedAuthority(SYNC_USER_PROFILE_SCOPE))
                          .jwt(token -> token.claim("aud", "test-client").build())))
            .andExpect(status().isOk());

        verify(userProfileService).requestSyncIdamUser("1234");

    }

}
