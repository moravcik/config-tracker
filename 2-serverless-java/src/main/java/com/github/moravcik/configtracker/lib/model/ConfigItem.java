package com.github.moravcik.configtracker.lib.model;

import com.github.moravcik.configtracker.lib.converter.ConfigAttributeConverter;
import com.github.moravcik.configtracker.lib.types.Config;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;

@DynamoDbBean
public class ConfigItem extends ConfigTableItem {
    private Config config;
    
    @DynamoDbConvertedBy(ConfigAttributeConverter.class)
    public Config getConfig() { return config; }
    public void setConfig(Config config) { this.config = config; }
}