package uk.gov.hmcts.idam.userprofilebridge.service;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;
import uk.gov.hmcts.idam.userprofilebridge.repository.InvitationEntityRepository;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationEntity;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationStatus;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationType;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.idam.userprofilebridge.service.UserEventService.UP_SYSTEM_CATEGORIES;

public class InvitationService {

    private final InvitationEntityRepository invitationEntityRepository;

    private final UserEventService userEventService;

    @Value("${idam.invitations-since-duration}")
    private Duration invitationsSinceDuration;

    private Clock clock;

    public InvitationService(InvitationEntityRepository invitationEntityRepository, UserEventService userEventService) {
        this.invitationEntityRepository = invitationEntityRepository;
        this.userEventService = userEventService;
        this.clock = Clock.system(ZoneOffset.UTC);
    }

    @VisibleForTesting
    protected void changeClock(Clock clock) {
        this.clock = clock;
    }

    public void createEventsForAcceptedInvitations() {

        Timestamp createdSince = Timestamp.from(clock.instant().minus(invitationsSinceDuration));

        Slice<InvitationEntity> invitationsSlice;

        Pageable nextPage = PageRequest.of(0, 10);

        do {
            invitationsSlice = invitationEntityRepository
                .findByLastModifiedAfterAndInvitationStatusAndInvitationTypeInOrderByLastModifiedAsc(
                    createdSince,
                    InvitationStatus.ACCEPTED,
                    List.of(InvitationType.INVITE, InvitationType.REACTIVATE),
                    nextPage);

            if (invitationsSlice.hasContent()) {
                invitationsSlice.get().forEach(this::createEventForInvitation);
            }

            nextPage = invitationsSlice.nextPageable();

        } while (invitationsSlice.hasNext());

    }

    public void createEventForInvitation(InvitationEntity invitation) {
        Set<UserProfileCategory> invitationCategories =
            userEventService.getUserProfileCategories(invitation.getActivationRoleNames());
        if (CollectionUtils.containsAny(invitationCategories, UP_SYSTEM_CATEGORIES)) {
            
        }
    }

}
