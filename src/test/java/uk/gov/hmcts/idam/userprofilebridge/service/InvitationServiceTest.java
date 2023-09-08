package uk.gov.hmcts.idam.userprofilebridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.idam.userprofilebridge.model.UserProfileCategory;
import uk.gov.hmcts.idam.userprofilebridge.repository.InvitationEntityRepository;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationEntity;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationStatus;
import uk.gov.hmcts.idam.userprofilebridge.repository.model.InvitationType;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final long EPOCH_1AM = 3600;

    @Mock
    InvitationEntityRepository invitationEntityRepository;

    @Mock
    UserEventService userEventService;

    @Mock
    UserProfileService userProfileService;

    @InjectMocks
    InvitationService underTest;

    Clock testClock = Clock.fixed(Instant.ofEpochSecond(EPOCH_1AM), ZoneOffset.UTC);

    @BeforeEach
    public void setup() {
        underTest.changeClock(testClock);
        ReflectionTestUtils.setField(underTest, "batchSize", 2);
        ReflectionTestUtils.setField(underTest, "invitationsSinceDuration", Duration.ofMinutes(10));
    }

    @Test
    public void createEventsForAcceptedInvitations_singleEvent() {
        InvitationEntity testInvitation = buildInvitationEntity("1");
        Pageable pageable = mock(Pageable.class);
        Timestamp timestamp = Timestamp.from(testClock.instant().minus(10, ChronoUnit.MINUTES));
        List<InvitationType> invitationTypes = List.of(InvitationType.INVITE, InvitationType.REACTIVATE);
        when(invitationEntityRepository.findByLastModifiedAfterAndInvitationStatusAndInvitationTypeInOrderByLastModifiedAsc(
            eq(timestamp),
            eq(InvitationStatus.ACCEPTED),
            eq(invitationTypes),
            argThat(p -> (p.getPageSize() == 2 && p.getPageNumber() == 0))
        )).thenReturn(new SliceImpl<InvitationEntity>(List.of(testInvitation), pageable, false));
        when(userEventService.getUserProfileCategories(anyList())).thenReturn(Set.of(UserProfileCategory.CASEWORKER));

        underTest.createEventsForAcceptedInvitations();

        verify(userProfileService, times(1)).requestAddIdamUser("test-user-id-1", "test-client-id-1");
    }

    @Test
    public void createEventsForAcceptedInvitations_twoPages() {
        InvitationEntity testInvitation1 = buildInvitationEntity("1");
        InvitationEntity testInvitation2 = buildInvitationEntity("2");
        InvitationEntity testInvitation3 = buildInvitationEntity("3");
        Pageable pageable = mock(Pageable.class);
        Timestamp timestamp = Timestamp.from(testClock.instant().minus(10, ChronoUnit.MINUTES));
        List<InvitationType> invitationTypes = List.of(InvitationType.INVITE, InvitationType.REACTIVATE);
        //noinspection unchecked
        when(invitationEntityRepository
                 .findByLastModifiedAfterAndInvitationStatusAndInvitationTypeInOrderByLastModifiedAsc(
                     eq(timestamp),
                     eq(InvitationStatus.ACCEPTED),
                     eq(invitationTypes),
                     any()
        )).thenReturn(
            new SliceImpl<>(List.of(testInvitation1, testInvitation2), pageable, true),
            new SliceImpl<>(List.of(testInvitation3), pageable, false)
        );

        when(userEventService.getUserProfileCategories(anyList())).thenReturn(Set.of(UserProfileCategory.CASEWORKER));

        underTest.createEventsForAcceptedInvitations();

        verify(userProfileService, times(1)).requestAddIdamUser("test-user-id-1", "test-client-id-1");
        verify(userProfileService, times(1)).requestAddIdamUser("test-user-id-2", "test-client-id-2");
        verify(userProfileService, times(1)).requestAddIdamUser("test-user-id-3", "test-client-id-3");


    }

    private InvitationEntity buildInvitationEntity(String postfix) {
        InvitationEntity testInvitation = new InvitationEntity();
        testInvitation.setId("test-invitation-id-" + postfix);
        testInvitation.setUserId("test-user-id-" + postfix);
        testInvitation.setActivationRoleNames(List.of("caseworker"));
        testInvitation.setClientId("test-client-id-" + postfix);
        return testInvitation;
    }

}
