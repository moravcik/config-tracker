package com.github.moravcik.configtracker.lib.repository;

import com.github.moravcik.configtracker.lib.model.ConfigChangeItem;
import com.github.moravcik.configtracker.lib.model.ConfigItem;
import com.github.moravcik.configtracker.lib.model.ConfigPathChangeItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigRepository {
    
    private final DynamoDbTable<ConfigItem> configTable;
    private final DynamoDbTable<ConfigChangeItem> configChangeTable;
    private final DynamoDbTable<ConfigPathChangeItem> configPathChangeTable;
    
    public ConfigRepository(String tableName) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.create())
                .build();
        
        // All point to same physical table but with different schemas
        this.configTable = enhancedClient.table(tableName, TableSchema.fromBean(ConfigItem.class));
        this.configChangeTable = enhancedClient.table(tableName, TableSchema.fromBean(ConfigChangeItem.class));
        this.configPathChangeTable = enhancedClient.table(tableName, TableSchema.fromBean(ConfigPathChangeItem.class));
    }
    
    public void saveConfig(ConfigItem item) {
        configTable.putItem(item);
    }
    
    public List<ConfigItem> getConfigsByConfigId(String configId) {
        return configTable.query(QueryConditional.keyEqualTo(k -> k.partitionValue("CONFIG#" + configId)))
                .items()
                .stream()
                .collect(Collectors.toList());
    }
}