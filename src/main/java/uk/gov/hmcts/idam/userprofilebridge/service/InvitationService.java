package uk.gov.hmcts.idam.userprofilebridge.service;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;
import uk.gov.hmcts.idam.userprofilebridge.repository.InvitationEntityRepository;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationEntity;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationStatus;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationType;
import uk.gov.hmcts.idam.userprofilebridge.trace.TraceAttribute;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.idam.userprofilebridge.service.UserEventService.UP_SYSTEM_CATEGORIES;

@Service
@Slf4j
@ConditionalOnProperty(name = "scheduler.enabled", matchIfMissing = true)
public class InvitationService {

    private final InvitationEntityRepository invitationEntityRepository;

    private final UserEventService userEventService;

    private final UserProfileService userProfileService;

    @Value("${scheduler.invitations.invitations-since-duration:1h}")
    private Duration invitationsSinceDuration;

    @Value("${scheduler.invitations.batch-size:100}")
    private Integer batchSize;

    private Clock clock;

    public InvitationService(InvitationEntityRepository invitationEntityRepository,
                             UserEventService userEventService,
                             UserProfileService userProfileService) {
        this.invitationEntityRepository = invitationEntityRepository;
        this.userEventService = userEventService;
        this.userProfileService = userProfileService;
        this.clock = Clock.system(ZoneOffset.UTC);
    }

    @VisibleForTesting
    protected void changeClock(Clock clock) {
        this.clock = clock;
    }

    public void createEventsForAcceptedInvitations() {
        Timestamp createdSince = Timestamp.from(clock.instant().minus(invitationsSinceDuration));
        Slice<InvitationEntity> invitationsSlice;
        Pageable nextPage = PageRequest.of(0, batchSize);

        int count = 0;
        do {
            invitationsSlice =
                invitationEntityRepository.findByLastModifiedAfterAndInvitationStatusAndInvitationTypeInOrderByLastModifiedAsc(
                    createdSince,
                    InvitationStatus.ACCEPTED,
                    List.of(InvitationType.INVITE, InvitationType.REACTIVATE),
                    nextPage
                );

            if (invitationsSlice.hasContent()) {
                invitationsSlice.get().forEach(this::createEventForInvitation);
                count += invitationsSlice.getNumberOfElements();
            }

            nextPage = invitationsSlice.nextPageable();

        } while (invitationsSlice.hasNext());

        Span.current().setAttribute(TraceAttribute.COUNT, String.valueOf(count));

    }

    public void createEventForInvitation(InvitationEntity invitation) {
        Set<UserProfileCategory> invitationCategories =
            userEventService.getUserProfileCategories(invitation.getActivationRoleNames());
        if (CollectionUtils.containsAny(invitationCategories, UP_SYSTEM_CATEGORIES)) {
            log.info("Handling invitation for {}, with categories {}", invitation.getId(), invitationCategories);
            userProfileService.requestAddIdamUser(invitation.getUserId(), invitation.getClientId());
        } else {
            log.info("Skipping events for invitation {}, no profile related categories", invitation.getId());
        }
    }

}
