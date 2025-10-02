package com.github.moravcik.configtracker.lib.lambda;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.moravcik.configtracker.lib.model.ConfigChangeItem;
import com.github.moravcik.configtracker.lib.model.ConfigItem;
import com.github.moravcik.configtracker.lib.model.ConfigPathChangeItem;
import com.github.moravcik.configtracker.lib.model.ConfigTableItem;
import com.github.moravcik.configtracker.lib.types.ConfigPathChange;
import com.github.moravcik.configtracker.lib.utils.DynamoUtils;
import com.github.moravcik.configtracker.lib.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class ConfigTableStreamHandler implements Function<DynamodbEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigTableStreamHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();


    private final SnsClient snsClient = SnsClient.create();
    private final String configChangesTopicArn = System.getenv("CONFIG_CHANGES_TOPIC_ARN");

    @Override
    public Void apply(DynamodbEvent event) {
        event.getRecords().forEach(this::processRecord);
        return null;
    }

    private void processRecord(DynamodbEvent.DynamodbStreamRecord record) {
        try {
            logger.info("Processing DynamoDB record: {}", record.getEventName());

            if (!"INSERT".equals(record.getEventName()) || record.getDynamodb().getNewImage() == null) {
                return;
            }

            Map<String, AttributeValue> item = record.getDynamodb().getNewImage();
            String entityType = item.get("entityType").getS();

            switch (entityType) {
                case "CONFIG":
                    handleInsertConfig(item.get("configId").getS());
                    break;
                case "CONFIG_CHANGE":
                    handleInsertConfigChange(item);
                    break;
                default:
                    logger.info("No handler for entityType {}", entityType);
            }
        } catch (Exception e) {
            logger.error("Error processing record", e);
        }
    }

    private void handleInsertConfig(String configId) throws Exception {
        List<ConfigItem> items = DynamoUtils.getLatestConfigEntity(configId, 2);
        if (items.size() < 2) return;

        ConfigItem latest = items.get(0);
        ConfigItem secondLatest = items.get(1);

        List<ObjectUtils.Difference> pathChanges = ObjectUtils.calculateDifferences(
                secondLatest.getConfig(), latest.getConfig(), false);

        logger.info("Config {} has {} changes", configId, pathChanges.size());

        if (pathChanges.isEmpty()) return;

        List<ConfigPathChange> configPathChanges = pathChanges.stream()
                .map(diff -> new ConfigPathChange(
                        ConfigPathChange.ConfigPathChangeType.valueOf(diff.type.name()),
                        diff.path,
                        diff.oldValue,
                        diff.newValue))
                .toList();

        // Create config change item
        ConfigChangeItem configChangeItem = new ConfigChangeItem();
        configChangeItem.setPk("CONFIG_CHANGE#" + configId);
        configChangeItem.setSk(latest.getTimestamp());
        configChangeItem.setEntityType(ConfigTableItem.EntityType.CONFIG_CHANGE);
        configChangeItem.setConfigId(configId);
        configChangeItem.setTimestamp(latest.getTimestamp());
        configChangeItem.setPathChanges(configPathChanges);

        // Create path change items
        List<ConfigPathChangeItem> pathChangeItems = new ArrayList<>();
        for (ConfigPathChange change : configPathChanges) {
            ConfigPathChangeItem pathChangeItem = new ConfigPathChangeItem();
            pathChangeItem.setPk("CONFIG_PATH_CHANGE#" + configId);
            pathChangeItem.setSk(change.getType() + "#" + latest.getTimestamp() + "#" + change.getPath());
            pathChangeItem.setEntityType(ConfigTableItem.EntityType.CONFIG_PATH_CHANGE);
            pathChangeItem.setConfigId(configId);
            pathChangeItem.setType(change.getType());
            pathChangeItem.setPath(change.getPath());
            pathChangeItem.setOldValue(change.getOldValue());
            pathChangeItem.setNewValue(change.getNewValue());
            pathChangeItem.setTimestamp(latest.getTimestamp());
            pathChangeItems.add(pathChangeItem);
        }

        // Write config change item
        DynamoUtils.getEnhancedClient().table(System.getenv("CONFIG_TABLE_NAME"), 
                TableSchema.fromBean(ConfigChangeItem.class))
                .putItem(configChangeItem);

        // Batch write path change items using raw DynamoDB
        List<WriteRequest> writeRequests = pathChangeItems.stream()
                .map(item -> WriteRequest.builder()
                        .putRequest(PutRequest.builder()
                                .item(Map.of(
                                        "pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(item.getPk()).build(),
                                        "sk", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(item.getSk()).build(),
                                        "entityType", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(item.getEntityType().name()).build(),
                                        "configId", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(item.getConfigId()).build(),
                                        "type", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(item.getType().name()).build(),
                                        "path", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(item.getPath()).build(),
                                        "oldValue", serializeValueForDynamoDB(item.getOldValue()),
                                        "newValue", serializeValueForDynamoDB(item.getNewValue()),
                                        "timestamp", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(item.getTimestamp()).build()
                                ))
                                .build())
                        .build())
                .toList();

        DynamoUtils.getDynamoDbClient().batchWriteItem(BatchWriteItemRequest.builder()
                .requestItems(Map.of(System.getenv("CONFIG_TABLE_NAME"), writeRequests))
                .build());

        logger.info("Config change and path changes written successfully");
    }

    private void handleInsertConfigChange(Map<String, AttributeValue> item) throws Exception {
        Map<String, Object> configChangeToSend = stripDbKeys(item);

        snsClient.publish(PublishRequest.builder()
                .topicArn(configChangesTopicArn)
                .message(objectMapper.writeValueAsString(configChangeToSend))
                .build());

        logger.info("TODO - Published config change to SNS");
    }

    private software.amazon.awssdk.services.dynamodb.model.AttributeValue serializeValueForDynamoDB(Object value) {
        if (value == null) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().nul(true).build();
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(value.toString()).build();
        }
        try {
            Map<String, Object> objectMap = objectMapper.convertValue(value, Map.class);
            return convertMapToDynamoDBAttributeValue(objectMap);
        } catch (Exception e) {
            return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(value.toString()).build();
        }
    }

    private software.amazon.awssdk.services.dynamodb.model.AttributeValue convertMapToDynamoDBAttributeValue(Map<String, Object> map) {
        Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> attributeMap = map.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> serializeValueForDynamoDB(entry.getValue())
                ));
        return software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().m(attributeMap).build();
    }

    private Map<String, Object> stripDbKeys(Map<String, AttributeValue> item) {
        return Map.of(
                "configId", item.get("configId"),
                "timestamp", item.get("timestamp"),
                "pathChanges", item.get("pathChanges")
        );
    }
}