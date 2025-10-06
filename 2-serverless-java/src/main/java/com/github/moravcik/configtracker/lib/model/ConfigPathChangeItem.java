package com.github.moravcik.configtracker.lib.model;

import com.github.moravcik.configtracker.lib.types.ConfigPathChange;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class ConfigPathChangeItem extends ConfigTableItem {
    private ConfigPathChange.ConfigPathChangeType type;
    private String path;
    private Object oldValue;
    private Object newValue;
    
    public ConfigPathChange.ConfigPathChangeType getType() { return type; }
    public void setType(ConfigPathChange.ConfigPathChangeType type) { this.type = type; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object oldValue) { this.oldValue = oldValue; }
    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }
}