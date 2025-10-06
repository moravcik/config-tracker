package com.github.moravcik.configtracker.lib.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.moravcik.configtracker.lib.types.ConfigPathChange;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigPathChangeListConverter implements AttributeConverter<List<ConfigPathChange>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(List<ConfigPathChange> input) {
        List<AttributeValue> items = input.stream()
                .map(change -> AttributeValue.builder().m(Map.of(
                        "type", AttributeValue.builder().s(change.getType().name()).build(),
                        "path", AttributeValue.builder().s(change.getPath()).build(),
                        "oldValue", serializeValue(change.getOldValue()),
                        "newValue", serializeValue(change.getNewValue())
                )).build())
                .collect(Collectors.toList());
        return AttributeValue.builder().l(items).build();
    }

    @Override
    public List<ConfigPathChange> transformTo(AttributeValue input) {
        return input.l().stream()
                .map(item -> {
                    Map<String, AttributeValue> map = item.m();
                    ConfigPathChange change = new ConfigPathChange();
                    change.setType(ConfigPathChange.ConfigPathChangeType.valueOf(map.get("type").s()));
                    change.setPath(map.get("path").s());
                    change.setOldValue(deserializeValue(map.get("oldValue")));
                    change.setNewValue(deserializeValue(map.get("newValue")));
                    return change;
                })
                .collect(Collectors.toList());
    }

    private AttributeValue serializeValue(Object value) {
        if (value == null) {
            return AttributeValue.builder().nul(true).build();
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return AttributeValue.builder().s(value.toString()).build();
        }
        try {
            Map<String, Object> objectMap = objectMapper.convertValue(value, Map.class);
            return convertMapToAttributeValue(objectMap);
        } catch (Exception e) {
            return AttributeValue.builder().s(value.toString()).build();
        }
    }

    private Object deserializeValue(AttributeValue attributeValue) {
        if (attributeValue.nul() != null && attributeValue.nul()) {
            return null;
        }
        if (attributeValue.s() != null) {
            return attributeValue.s();
        }
        if (attributeValue.m() != null) {
            return convertAttributeValueToMap(attributeValue.m());
        }
        return attributeValue.toString();
    }

    private AttributeValue convertMapToAttributeValue(Map<String, Object> map) {
        Map<String, AttributeValue> attributeMap = map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> serializeValue(entry.getValue())
                ));
        return AttributeValue.builder().m(attributeMap).build();
    }

    private Map<String, Object> convertAttributeValueToMap(Map<String, AttributeValue> attributeMap) {
        return attributeMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> deserializeValue(entry.getValue())
                ));
    }

    @Override
    public EnhancedType<List<ConfigPathChange>> type() {
        return EnhancedType.listOf(ConfigPathChange.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.L;
    }
}