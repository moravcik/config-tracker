package com.github.moravcik.configtracker.lib.utils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ApiUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static APIGatewayProxyResponseEvent createSuccessResponse(Object obj) throws JsonProcessingException {
        return createSuccessResponse(objectMapper.writeValueAsString(obj));
    }

    public static APIGatewayProxyResponseEvent createSuccessResponse(String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(body)
                .withHeaders(Map.of("Content-Type", "application/json"));
    }

    public static APIGatewayProxyResponseEvent createErrorResponse(String message, int statusCode) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody("{\"error\":\"" + message + "\"}")
                .withHeaders(Map.of("Content-Type", "application/json"));
    }
}