package com.github.moravcik.configtracker.lib.utils;

import com.github.moravcik.configtracker.lib.model.ConfigItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

public class DynamoUtils {

    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    private static final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();

    private static final DynamoDbTable<ConfigItem> configTable = enhancedClient.table(
            System.getenv("CONFIG_TABLE_NAME"),
            TableSchema.fromBean(ConfigItem.class)
    );

    public static List<ConfigItem> getLatestConfigEntity(String configId, int limit) {
        return configTable.query(q -> q
                .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue("CONFIG#" + configId)))
                .scanIndexForward(false)
                .limit(limit))
                .items()
                .stream()
                .toList();
    }

    public static List<ConfigItem> getLatestConfigEntity(String configId) {
        return getLatestConfigEntity(configId, 1);
    }

    public static DynamoDbClient getDynamoDbClient() { return dynamoDbClient; }

    public static DynamoDbEnhancedClient getEnhancedClient() {
        return enhancedClient;
    }

    public static DynamoDbTable<ConfigItem> getConfigTable() {
        return configTable;
    }

}