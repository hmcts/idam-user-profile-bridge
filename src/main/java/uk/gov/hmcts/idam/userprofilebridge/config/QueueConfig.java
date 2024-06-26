package uk.gov.hmcts.idam.userprofilebridge.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import io.opentelemetry.api.trace.Span;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
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

        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addSerializer(ZonedDateTime.class,
                                 new ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        );

        ObjectMapper objectMapper = new ObjectMapper();
        // default settings for MappingJackson2MessageConverter
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(timeModule);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of("idam.userevent", UserEvent.class));
        converter.setObjectMapper(objectMapper);

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
