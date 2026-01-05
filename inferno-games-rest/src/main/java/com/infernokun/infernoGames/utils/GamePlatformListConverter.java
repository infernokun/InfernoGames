package com.infernokun.infernoGames.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infernokun.infernoGames.models.enums.GamePlatform;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Converter
public class GamePlatformListConverter implements AttributeConverter<List<GamePlatform>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<GamePlatform> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting platform list to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    @Override
    public List<GamePlatform> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || dbData.equals("null")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<GamePlatform>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to platform list: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
