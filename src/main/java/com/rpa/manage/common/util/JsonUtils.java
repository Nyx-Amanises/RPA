package com.rpa.manage.common.util;

import com.rpa.manage.common.exception.BusinessException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String toJsonString(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new BusinessException("JSON 数据序列化失败");
        }
    }

    public static JsonNode toJsonNode(ObjectMapper objectMapper, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException ex) {
            throw new BusinessException("JSON 数据解析失败");
        }
    }
}
