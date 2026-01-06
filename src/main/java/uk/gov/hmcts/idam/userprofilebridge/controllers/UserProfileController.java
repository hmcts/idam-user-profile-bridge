package uk.gov.hmcts.idam.userprofilebridge.controllers;

import io.opentelemetry.api.trace.Span;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.cft.rd.model.JudicialUserProfile;
import uk.gov.hmcts.cft.rd.model.UserProfile;
import uk.gov.hmcts.idam.userprofilebridge.service.UserProfileService;
import uk.gov.hmcts.idam.userprofilebridge.trace.TraceAttribute;

import static uk.gov.hmcts.idam.userprofilebridge.util.PrincipalHelper.getClientId;

@RestController
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/idam/api/v2/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_view-user-profile')")
    @SecurityRequirement(name = "bearerAuth")
    public User getUserById(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                            @PathVariable String userId) {
        Span.current().setAttribute(TraceAttribute.CLIENT_ID, getClientId(principal).orElse("n/a"));
        return userProfileService.getUserById(userId);
    }

    @GetMapping("/rd/api/v1/userprofile/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_view-user-profile')")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfile getUserProfileById(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                                          @PathVariable String userId) {
        Span.current().setAttribute(TraceAttribute.CLIENT_ID, getClientId(principal).orElse("n/a"));
        return userProfileService.getUserProfileById(userId);
    }

    @GetMapping("/rd/case-worker/profile/search-by-id/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_view-user-profile')")
    @SecurityRequirement(name = "bearerAuth")
    public CaseWorkerProfile getCaseworkerProfileById(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                                                      @PathVariable String userId) {
        Span.current().setAttribute(TraceAttribute.CLIENT_ID, getClientId(principal).orElse("n/a"));
        return userProfileService.getCaseWorkerProfileById(userId);
    }

    @GetMapping("rd/judicial/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_view-user-profile')")
    @SecurityRequirement(name = "bearerAuth")
    public JudicialUserProfile getJudicialUserProfile(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                                                      @PathVariable String userId) {
        Span.current().setAttribute(TraceAttribute.CLIENT_ID, getClientId(principal).orElse("n/a"));
        return null;
    }

    @PutMapping("/bridge/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('SCOPE_sync-user-profile')")
    @SecurityRequirement(name = "bearerAuth")
    public void syncIdamUser(@AuthenticationPrincipal @Parameter(hidden = true) Jwt principal,
                             @PathVariable String userId) {
        String clientId = getClientId(principal).orElse(null);
        Span.current().setAttribute(TraceAttribute.CLIENT_ID, clientId != null ? clientId : "n/a");
        userProfileService.requestSyncIdamUser(userId, clientId);
    }


}
