package com.github.moravcik.configtracker.lib.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.moravcik.configtracker.lib.types.Config;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ConfigAttributeConverter implements AttributeConverter<Config> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(Config input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        try {
            return AttributeValue.builder().s(objectMapper.writeValueAsString(input)).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Config", e);
        }
    }

    @Override
    public Config transformTo(AttributeValue input) {
        if (input.nul() != null && input.nul()) {
            return null;
        }
        try {
            return objectMapper.readValue(input.s(), Config.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize Config", e);
        }
    }

    @Override
    public EnhancedType<Config> type() {
        return EnhancedType.of(Config.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}