package uk.gov.hmcts.idam.userprofilebridge.config;

import io.opentelemetry.api.trace.Span;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.hmcts.idam.userprofilebridge.error.ListenerErrorHandler;
import uk.gov.hmcts.idam.userprofilebridge.messaging.model.UserEvent;
import uk.gov.hmcts.idam.userprofilebridge.trace.TraceAttribute;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@EnableJms
@Configuration
@Slf4j
public class QueueConfig {

    @Value("${idam.messaging.useTopics:true}")
    boolean useTopics;

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {

        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(ZonedDateTime.class,
                                 new ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        );

        JsonMapper objectMapper = JsonMapper.builder()
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(timeModule)
            .build();

        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of("idam.userevent", UserEvent.class));

        return converter;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                          ListenerErrorHandler errorHandler,
                                                                          MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setPubSubDomain(useTopics);
        factory.setSubscriptionDurable(useTopics);
        factory.setSubscriptionShared(useTopics);
        factory.setConnectionFactory(connectionFactory);
        factory.setErrorHandler(errorHandler);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrency("3-10");
        factory.setExceptionListener(e -> {
            Span.current().setAttribute(TraceAttribute.ERROR, "exception: " + e.getClass() + ": " + e.getMessage());
            log.info("JMS Listener Exception: {}: {}", e.getClass(), e.getMessage(), e);
        });
        factory.setErrorHandler(t -> {
            Span.current().setAttribute(TraceAttribute.ERROR, "error: " + t.getClass() + ": " + t.getMessage());
            log.error("JMS Listener Error: {}: {}", t.getClass(), t.getMessage(), t);
            try {
                throw t;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        return factory;
    }

}
