package com.github.moravcik.configtracker.lib.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.moravcik.configtracker.lib.model.ConfigChangeItem;
import com.github.moravcik.configtracker.lib.model.ConfigPathChangeItem;
import com.github.moravcik.configtracker.lib.utils.ApiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigChangeApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigChangeApiHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> ALLOWED_PARAMS = Set.of("type", "path", "timestampFrom", "timestampTo");

    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder()
                    .connectionTimeout(java.time.Duration.ofSeconds(2))
                    .socketTimeout(java.time.Duration.ofSeconds(5))
                    .build())
            .build();
    private static final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();
    private static final DynamoDbTable<ConfigChangeItem> configChangeTable = enhancedClient.table(
            System.getenv("CONFIG_TABLE_NAME"),
            TableSchema.fromBean(ConfigChangeItem.class)
    );

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            String httpMethod = event.getHttpMethod().toUpperCase();
            String configId = event.getPathParameters() != null ? event.getPathParameters().get("configId") : null;
            Map<String, String> queryParams = event.getQueryStringParameters() != null ? 
                    event.getQueryStringParameters() : new HashMap<>();

            logger.info("Config changes API: {}:{} {} {}", httpMethod, event.getPath(), configId, queryParams);

            // Validate query parameters
            Set<String> invalidParams = queryParams.keySet().stream()
                    .filter(p -> !ALLOWED_PARAMS.contains(p))
                    .collect(Collectors.toSet());

            if (!invalidParams.isEmpty()) {
                return ApiUtils.createErrorResponse("Invalid query parameters: " + String.join(", ", invalidParams), 400);
            }

            List<ConfigChangeItem> configChangeItems = new ArrayList<>();

            if ("GET".equals(httpMethod) && configId != null) {
                String type = queryParams.get("type");
                String path = queryParams.get("path");
                String timestampFrom = queryParams.get("timestampFrom");
                String timestampTo = queryParams.get("timestampTo");

                if (type != null || path != null) {
                    configChangeItems = queryByConfigPathChanges(configId, type, path, timestampFrom, timestampTo);
                } else {
                    configChangeItems = queryConfigChanges(configId, timestampFrom, timestampTo);
                }
            }

            List<Map<String, Object>> result = configChangeItems.stream()
                    .map(this::stripDbKeys)
                    .collect(Collectors.toList());

            return ApiUtils.createSuccessResponse(result);

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
            return ApiUtils.createErrorResponse("Internal server error", 500);
        }
    }

    private List<ConfigChangeItem> queryConfigChanges(String configId, String timestampFrom, String timestampTo) {
        List<ConfigChangeItem> items = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        do {
            QueryRequest.Builder queryBuilder = QueryRequest.builder()
                    .tableName(System.getenv("CONFIG_TABLE_NAME"))
                    .keyConditionExpression("pk = :pk" + buildTimestampCondition(timestampFrom, timestampTo))
                    .expressionAttributeValues(buildAttributeValues(configId, "CONFIG_CHANGE", null, null, timestampFrom, timestampTo));

            if (lastEvaluatedKey != null) {
                queryBuilder.exclusiveStartKey(lastEvaluatedKey);
            }

            QueryResponse response = dynamoDbClient.query(queryBuilder.build());
            lastEvaluatedKey = response.lastEvaluatedKey();

            response.items().forEach(item -> {
                try {
                    ConfigChangeItem configChangeItem = convertToConfigChangeItem(item);
                    items.add(configChangeItem);
                } catch (Exception e) {
                    logger.error("Error converting item", e);
                }
            });

        } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        return items;
    }

    private List<ConfigChangeItem> queryByConfigPathChanges(String configId, String type, String path, 
            String timestampFrom, String timestampTo) {
        
        // Query ConfigPathChangeItems
        List<ConfigPathChangeItem> pathChangeItems = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        do {
            QueryRequest.Builder queryBuilder = QueryRequest.builder()
                    .tableName(System.getenv("CONFIG_TABLE_NAME"))
                    .keyConditionExpression("pk = :pk" + (type != null ? " AND begins_with(sk, :type)" : ""))
                    .expressionAttributeValues(buildAttributeValues(configId, "CONFIG_PATH_CHANGE", type, path, timestampFrom, timestampTo));

            // Build filter expression
            List<String> filterExpressions = new ArrayList<>();
            Map<String, String> attributeNames = new HashMap<>();

            if (path != null) {
                filterExpressions.add("contains(#path, :path)");
                attributeNames.put("#path", "path");
            }
            if (timestampFrom != null && timestampTo != null) {
                filterExpressions.add("(#timestamp BETWEEN :tsFrom AND :tsTo)");
                attributeNames.put("#timestamp", "timestamp");
            } else if (timestampFrom != null) {
                filterExpressions.add("#timestamp >= :tsFrom");
                attributeNames.put("#timestamp", "timestamp");
            } else if (timestampTo != null) {
                filterExpressions.add("#timestamp <= :tsTo");
                attributeNames.put("#timestamp", "timestamp");
            }

            if (!filterExpressions.isEmpty()) {
                queryBuilder.filterExpression(String.join(" AND ", filterExpressions));
            }
            if (!attributeNames.isEmpty()) {
                queryBuilder.expressionAttributeNames(attributeNames);
            }
            if (lastEvaluatedKey != null) {
                queryBuilder.exclusiveStartKey(lastEvaluatedKey);
            }

            QueryResponse response = dynamoDbClient.query(queryBuilder.build());
            lastEvaluatedKey = response.lastEvaluatedKey();

            response.items().forEach(item -> {
                try {
                    ConfigPathChangeItem pathChangeItem = convertToConfigPathChangeItem(item);
                    pathChangeItems.add(pathChangeItem);
                } catch (Exception e) {
                    logger.error("Error converting path change item", e);
                }
            });

        } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

        // Get unique timestamps
        Set<String> timestamps = pathChangeItems.stream()
                .map(ConfigPathChangeItem::getTimestamp)
                .collect(Collectors.toSet());

        if (timestamps.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch get ConfigChangeItems
        List<Map<String, AttributeValue>> keys = timestamps.stream()
                .map(timestamp -> Map.of(
                        "pk", AttributeValue.builder().s("CONFIG_CHANGE#" + configId).build(),
                        "sk", AttributeValue.builder().s(timestamp).build()))
                .collect(Collectors.toList());

        BatchGetItemRequest batchRequest = BatchGetItemRequest.builder()
                .requestItems(Map.of(System.getenv("CONFIG_TABLE_NAME"), 
                        KeysAndAttributes.builder().keys(keys).build()))
                .build();

        BatchGetItemResponse batchResponse = dynamoDbClient.batchGetItem(batchRequest);
        
        return batchResponse.responses().get(System.getenv("CONFIG_TABLE_NAME")).stream()
                .map(this::convertToConfigChangeItem)
                .collect(Collectors.toList());
    }

    private String buildTimestampCondition(String timestampFrom, String timestampTo) {
        if (timestampFrom != null && timestampTo != null) {
            return " AND (sk BETWEEN :tsFrom AND :tsTo)";
        } else if (timestampFrom != null) {
            return " AND sk >= :tsFrom";
        } else if (timestampTo != null) {
            return " AND sk <= :tsTo";
        }
        return "";
    }

    private Map<String, AttributeValue> buildAttributeValues(String configId, String entityType, String type, 
            String path, String timestampFrom, String timestampTo) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":pk", AttributeValue.builder().s(entityType + "#" + configId).build());
        
        if (type != null) {
            values.put(":type", AttributeValue.builder().s(type + "#").build());
        }
        if (path != null) {
            values.put(":path", AttributeValue.builder().s(path).build());
        }
        if (timestampFrom != null) {
            values.put(":tsFrom", AttributeValue.builder().s(timestampFrom).build());
        }
        if (timestampTo != null) {
            values.put(":tsTo", AttributeValue.builder().s(timestampTo).build());
        }
        
        return values;
    }

    private ConfigChangeItem convertToConfigChangeItem(Map<String, AttributeValue> item) {
        ConfigChangeItem configChangeItem = new ConfigChangeItem();
        configChangeItem.setPk(item.get("pk").s());
        configChangeItem.setSk(item.get("sk").s());
        configChangeItem.setConfigId(item.get("configId").s());
        configChangeItem.setTimestamp(item.get("timestamp").s());
        
        // Convert pathChanges from DynamoDB format
        if (item.containsKey("pathChanges") && item.get("pathChanges").l() != null) {
            try {
                List<com.github.moravcik.configtracker.lib.types.ConfigPathChange> pathChanges = 
                    item.get("pathChanges").l().stream()
                        .map(this::convertToConfigPathChange)
                        .collect(java.util.stream.Collectors.toList());
                configChangeItem.setPathChanges(pathChanges);
            } catch (Exception e) {
                logger.error("Error converting pathChanges", e);
            }
        }
        
        return configChangeItem;
    }

    private com.github.moravcik.configtracker.lib.types.ConfigPathChange convertToConfigPathChange(AttributeValue item) {
        Map<String, AttributeValue> map = item.m();
        com.github.moravcik.configtracker.lib.types.ConfigPathChange change = 
            new com.github.moravcik.configtracker.lib.types.ConfigPathChange();
        
        change.setType(com.github.moravcik.configtracker.lib.types.ConfigPathChange.ConfigPathChangeType
            .valueOf(map.get("type").s()));
        change.setPath(map.get("path").s());
        change.setOldValue(convertAttributeValueToObject(map.get("oldValue")));
        change.setNewValue(convertAttributeValueToObject(map.get("newValue")));
        
        return change;
    }

    private Object convertAttributeValueToObject(AttributeValue attributeValue) {
        if (attributeValue.nul() != null && attributeValue.nul()) {
            return null;
        }
        if (attributeValue.s() != null) {
            return attributeValue.s();
        }
        if (attributeValue.m() != null) {
            return attributeValue.m().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> convertAttributeValueToObject(entry.getValue())
                ));
        }
        return attributeValue.toString();
    }

    private ConfigPathChangeItem convertToConfigPathChangeItem(Map<String, AttributeValue> item) {
        ConfigPathChangeItem pathChangeItem = new ConfigPathChangeItem();
        pathChangeItem.setPk(item.get("pk").s());
        pathChangeItem.setSk(item.get("sk").s());
        pathChangeItem.setConfigId(item.get("configId").s());
        pathChangeItem.setTimestamp(item.get("timestamp").s());
        pathChangeItem.setPath(item.get("path").s());
        // Note: Additional field conversions would be needed
        return pathChangeItem;
    }

    private Map<String, Object> stripDbKeys(ConfigChangeItem item) {
        return Map.of(
                "configId", item.getConfigId(),
                "timestamp", item.getTimestamp(),
                "pathChanges", item.getPathChanges() != null ? item.getPathChanges() : new ArrayList<>()
        );
    }


}