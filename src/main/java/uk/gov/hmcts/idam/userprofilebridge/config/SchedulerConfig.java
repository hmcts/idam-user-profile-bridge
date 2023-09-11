package uk.gov.hmcts.idam.userprofilebridge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.task.TaskSchedulerCustomizer;
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
@Slf4j
public class SchedulerConfig implements TaskSchedulerCustomizer {

    @Value("${DATASOURCE_HOST:not-found}")
    private String envDBHost;

    @Value("${rd.userprofile.client.registration.service-account-user:not-found}")
    private String springProperty;

    @Autowired
    InvitationService  invitationService;

    @Autowired
    private ErrorHandler schedulerErrorHandler;

    @Scheduled(initialDelayString = "${scheduler.initialDelayMs}",
        fixedRateString = "${scheduler.invitations.triggerFrequencyMs}")
    public void triggerCreateEventsForInvitations() {
        log.info("Env variable is {}", envDBHost);
        log.info("Spring prop is {}", springProperty);
        invitationService.createEventsForAcceptedInvitations();
    }

    @Override
    public void customize(ThreadPoolTaskScheduler taskScheduler) {
        taskScheduler.setErrorHandler(schedulerErrorHandler);
    }
}
