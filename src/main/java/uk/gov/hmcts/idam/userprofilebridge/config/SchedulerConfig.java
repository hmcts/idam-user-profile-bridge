package uk.gov.hmcts.idam.userprofilebridge.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;
import uk.gov.hmcts.idam.userprofilebridge.service.InvitationService;

@Configuration
@EnableScheduling
@EnableAsync
@ConditionalOnProperty(name = "scheduler.enabled", matchIfMissing = true)
public class SchedulerConfig implements ThreadPoolTaskSchedulerCustomizer {

    @Autowired
    InvitationService  invitationService;

    @Autowired
    private ErrorHandler schedulerErrorHandler;

    @Scheduled(initialDelayString = "${scheduler.initialDelayMs}",
        fixedRateString = "${scheduler.invitations.triggerFrequencyMs}")
    public void triggerCreateEventsForInvitations() {
        invitationService.createEventsForAcceptedInvitations();
    }

    @Override
    public void customize(ThreadPoolTaskScheduler taskScheduler) {
        taskScheduler.setErrorHandler(schedulerErrorHandler);
    }
}
