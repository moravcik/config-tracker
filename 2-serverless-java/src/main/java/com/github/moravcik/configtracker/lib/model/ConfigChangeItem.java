package com.github.moravcik.configtracker.lib.model;

import com.github.moravcik.configtracker.lib.converter.ConfigPathChangeListConverter;
import com.github.moravcik.configtracker.lib.types.ConfigPathChange;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;

import java.util.List;

@DynamoDbBean
public class ConfigChangeItem extends ConfigTableItem {
    private List<ConfigPathChange> pathChanges;
    
    @DynamoDbConvertedBy(ConfigPathChangeListConverter.class)
    public List<ConfigPathChange> getPathChanges() { return pathChanges; }
    public void setPathChanges(List<ConfigPathChange> pathChanges) { this.pathChanges = pathChanges; }
}