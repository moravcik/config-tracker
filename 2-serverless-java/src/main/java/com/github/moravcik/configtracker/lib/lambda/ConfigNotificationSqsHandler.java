package com.github.moravcik.configtracker.lib.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigNotificationSqsHandler implements RequestHandler<SQSEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigNotificationSqsHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
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