package com.github.moravcik.configtracker.lib.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.moravcik.configtracker.lib.model.ConfigItem;
import com.github.moravcik.configtracker.lib.model.ConfigTableItem;
import com.github.moravcik.configtracker.lib.types.Config;
import com.github.moravcik.configtracker.lib.utils.ApiUtils;
import com.github.moravcik.configtracker.lib.utils.DynamoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConfigApiHandler implements Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<ConfigItem> table = DynamoUtils.getConfigTable();

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(ConfigApiHandler.class);

    private static String formatTimestamp(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }



    private APIGatewayProxyResponseEvent handleSave(String configJson, String existingConfigId) throws Exception {
        String configId = existingConfigId != null ? existingConfigId : UUID.randomUUID().toString();
        String timestamp = formatTimestamp(Instant.now());
        ConfigItem item = new ConfigItem();

        item.setPk("CONFIG#" + configId);
        item.setSk(timestamp);
        item.setEntityType(ConfigTableItem.EntityType.CONFIG);
        item.setConfigId(configId);
        item.setTimestamp(timestamp);
        item.setConfig(objectMapper.readValue(configJson, Config.class));

        table.putItem(item);

        Map<String, Object> response = Map.of(
                "configId", configId,
                "timestamp", timestamp,
                "config", objectMapper.readValue(configJson, Object.class)
        );
        return ApiUtils.createSuccessResponse(response);
    }

    private APIGatewayProxyResponseEvent handleList() throws Exception {
        List<ConfigItem> allItems = table.scan(ScanEnhancedRequest.builder()
                .filterExpression(software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                        .expression("entityType = :et")
                        .putExpressionValue(":et", AttributeValue.builder().s("CONFIG").build())
                        .build())
                .build())
                .items()
                .stream()
                .toList();

        // Get latest config per configId
        Map<String, ConfigItem> latestConfigs = new HashMap<String, ConfigItem>();
        for (ConfigItem item : allItems) {
            String configId = item.getConfigId();
            String timestamp = item.getTimestamp();

            if (!latestConfigs.containsKey(configId) ||
                    timestamp.compareTo((String) latestConfigs.get(configId).getTimestamp()) > 0) {
                latestConfigs.put(configId, item);
            }
        }

        List<Map<String, Object>> result = latestConfigs.values().stream()
                .map(this::stripDbKeys)
                .collect(Collectors.toList());

        return ApiUtils.createSuccessResponse(result);
    }

    private APIGatewayProxyResponseEvent handleGet(String configId) throws Exception {
        List<ConfigItem> items = DynamoUtils.getLatestConfigEntity(configId);
        logger.info("Latest config by configId ({}): {}", configId, items.size());

        if (items.isEmpty()) {
            return ApiUtils.createErrorResponse("Config not found", 404);
        }

        Map<String, Object> result = stripDbKeys(items.get(0));
        return ApiUtils.createSuccessResponse(result);
    }

    private APIGatewayProxyResponseEvent handleUpdate(APIGatewayProxyRequestEvent event, String configId, boolean isPatch) throws Exception {
        List<ConfigItem> latestConfigs = DynamoUtils.getLatestConfigEntity(configId);

        if (latestConfigs.isEmpty()) {
            return ApiUtils.createErrorResponse("Config not found", 404);
        }

        Config existingConfig = latestConfigs.get(0).getConfig();
        String existingConfigJson = objectMapper.writeValueAsString(existingConfig);
        String updateBodyJson = event.getBody();

        String updatedConfigJson = isPatch ? mergeConfigs(existingConfig, updateBodyJson) : updateBodyJson;

        if (existingConfigJson.equals(updatedConfigJson)) {
            return ApiUtils.createErrorResponse("No update - Equal with latest Config version", 400);
        }
        return handleSave(updatedConfigJson, configId);
    }

    private String mergeConfigs(Config existingConfig, String updateJson) throws Exception {
        JsonNode existingNode = objectMapper.valueToTree(existingConfig);
        JsonNode updateNode = objectMapper.readTree(updateJson);
        JsonNode mergedNode = deepMerge(existingNode, updateNode);
        return objectMapper.writeValueAsString(mergedNode);
    }

    private JsonNode deepMerge(JsonNode existing, JsonNode update) {
        if (update.isNull()) return existing;
        if (existing.isNull() || !existing.isObject() || !update.isObject()) return update;
        
        ObjectNode merged = existing.deepCopy();
        update.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (value.isArray() || !merged.has(key) || !merged.get(key).isObject()) {
                merged.set(key, value);
            } else {
                merged.set(key, deepMerge(merged.get(key), value));
            }
        });
        return merged;
    }

    private Map<String, Object> stripDbKeys(ConfigItem item) {
        try {
            return Map.of(
                    "configId", item.getConfigId(),
                    "timestamp", item.getTimestamp(),
                    "config", item.getConfig()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public APIGatewayProxyResponseEvent apply(APIGatewayProxyRequestEvent event) {
        try {
            String httpMethod = event.getHttpMethod().toUpperCase();
            String configId = event.getPathParameters() != null ? event.getPathParameters().get("configId") : null;

            logger.info("Config API: {}:{} {}", httpMethod, event.getPath(), configId);

            switch (httpMethod) {
                case "POST":
                    if (configId == null) return handleSave(event.getBody(), null);
                    break;
                case "GET":
                    if (configId == null) return handleList();
                    else return handleGet(configId);
                case "PUT":
                    if (configId == null) return ApiUtils.createErrorResponse("Config ID not specified", 400);
                    else return handleUpdate(event, configId, false);
                case "PATCH":
                    if (configId == null) return ApiUtils.createErrorResponse("Config ID not specified", 400);
                    else return handleUpdate(event, configId, true);
            }
            return ApiUtils.createErrorResponse("Method/path not supported", 404);

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
            return ApiUtils.createErrorResponse("Internal server error", 500);
        }
    }
}
