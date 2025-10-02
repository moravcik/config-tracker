package com.github.moravcik.configtracker.lib.lambda;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ConfigNotificationSqsHandler implements Function<SQSEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigNotificationSqsHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Void apply(SQSEvent event) {
        event.getRecords().forEach(record -> {
            try {
                Object configChangeNotification = objectMapper.readValue(record.getBody(), Object.class);
                logger.info("Config change notification: {}", configChangeNotification);
                // TODO process the config change notification, e.g., send email or trigger other workflows
            } catch (Exception e) {
                logger.error("Error processing SQS record", e);
            }
        });
        return null;
    }
}