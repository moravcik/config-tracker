package com.github.moravcik.configtracker.lib.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

@DynamoDbBean
public abstract class ConfigTableItem {
    
    public enum EntityType {
        CONFIG, CONFIG_CHANGE, CONFIG_PATH_CHANGE
    }

    private String pk;
    private String sk;
    private EntityType entityType;
    private String configId;
    private String timestamp;

    @DynamoDbPartitionKey
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }
    
    @DynamoDbSortKey
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }
    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }


}