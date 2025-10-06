package com.github.moravcik.configtracker.lib.types;

public class ConfigPathChange {
    
    public enum ConfigPathChangeType {
        ADD, UPDATE, REMOVE, EQUAL
    }
    
    private ConfigPathChangeType type;
    private String path;
    private Object oldValue;
    private Object newValue;

    public ConfigPathChange() {}

    public ConfigPathChange(ConfigPathChangeType type, String path, Object oldValue, Object newValue) {
        this.type = type;
        this.path = path;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public ConfigPathChangeType getType() { return type; }
    public void setType(ConfigPathChangeType type) { this.type = type; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object oldValue) { this.oldValue = oldValue; }
    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }
}