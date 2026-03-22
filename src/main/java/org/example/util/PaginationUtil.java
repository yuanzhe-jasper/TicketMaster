package org.example.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class PaginationUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String encodeToken(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
            return null;
        }
        Map<String, String> simpleMap = new HashMap<>();
        lastEvaluatedKey.forEach((k, v) -> simpleMap.put(k, v.s()));
        try {
            String json = MAPPER.writeValueAsString(simpleMap);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode pagination token", e);
        }
    }

    public static Map<String, AttributeValue> decodeToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(token));
            Map<String, String> simpleMap = MAPPER.readValue(json, new TypeReference<>() {});
            Map<String, AttributeValue> key = new HashMap<>();
            simpleMap.forEach((k, v) -> key.put(k, AttributeValue.fromS(v)));
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Invalid pagination token", e);
        }
    }
}