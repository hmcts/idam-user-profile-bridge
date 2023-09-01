package uk.gov.hmcts.idam.userprofilebridge.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cft.idam.api.v2.common.model.User;
import uk.gov.hmcts.cft.rd.model.CaseWorkerProfile;
import uk.gov.hmcts.cft.rd.model.UserProfile;
import uk.gov.hmcts.idam.userprofilebridge.service.UserProfileService;

@RestController
@Slf4j
public class UserProfileController {

    @Value("${spring.security.oauth2.client.registration.idam-user-profile-bridge.client-secret}")
    private String idamSecretValue;

    @Value("${spring.security.oauth2.client.registration.rd-userprofile-api.client-secret}")
    private String rdSecretValue;

    @Value("${app-insights-connection-string}")
    private String insightConnString;

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/idam/api/v2/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_profile')")
    @SecurityRequirement(name = "bearerAuth")
    public User getUserById(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                            @PathVariable String userId) {
        log.info("Idam client secret is: {}", idamSecretValue);
        log.info("Rd client secret is: {}", rdSecretValue);
        log.info("insight conn string is: {}", insightConnString);
        return userProfileService.getUserById(userId);
    }

    @GetMapping("/rd/api/v1/userprofile/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_profile')")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile getUserProfileById(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                                          @PathVariable String userId) {
        return userProfileService.getUserProfileById(userId);
    }

    @GetMapping("/rd/case-worker/profile/search-by-id/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_profile')")
    @SecurityRequirement(name = "bearerAuth")
    public CaseWorkerProfile getCaseworkerProfileById(String userId) {
        return userProfileService.getCaseWorkerProfileById(userId);
    }

    @PutMapping("/bridge/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_profile')")
    @SecurityRequirement(name = "bearerAuth")
    public void syncIdamUser(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                                             @PathVariable String userId) {
        userProfileService.requestSyncIdamUser(userId);
    }


}
