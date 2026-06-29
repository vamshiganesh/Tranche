package com.tranche.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Shared JSON serialization for audit state and outbox payloads.
 */
public final class JsonMapConverter {

    private static final Logger log = LoggerFactory.getLogger(JsonMapConverter.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private JsonMapConverter() {
    }

    public static String toJson(ObjectMapper objectMapper, Map<String, Object> state) {
        if (state == null || state.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize JSON map", ex);
            return null;
        }
    }

    public static String toJsonRequired(ObjectMapper objectMapper, Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize JSON map", ex);
        }
    }

    public static Map<String, Object> fromJson(ObjectMapper objectMapper, String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse JSON map", ex);
            return Collections.emptyMap();
        }
    }
}
