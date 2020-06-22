package com.rickerlyman.iot.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Jackson {

    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected Jackson() {
    }

    public static <T> String toJsonString(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to write object as json string.", e);
        }
    }

    public static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Json String.", e);
        }
    }

    public static <T> T fromJsonString(byte[] json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            String msg = "Unable to parse Json String. " + (new String(json).isEmpty() ? " empty!" : new String(json));
            throw new IllegalArgumentException(msg, e);
        }
    }

    public static <T> T fromJsonString(InputStream stream, Class<T> clazz) {
        try {
            return objectMapper.readValue(stream, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Json String.", e);
        }
    }

    public static <T> T fromJsonString(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Json String.", e);
        }
    }


    public static <T> T fromJsonString(byte[] json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Json String.", e);
        }
    }

    public static <T> T fromJsonString(InputStream stream, TypeReference<T> type) {
        try {
            return objectMapper.readValue(stream, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Json String.", e);
        }
    }

    public static <T> List<T> fromJsonStringList(InputStream stream, Class<T> type) {
        try {
            return objectMapper.readValue(stream, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Json String.", e);
        }
    }

    public static <T> T convertValue(Object object, Class<T> clazz) {
        try {
            return objectMapper.convertValue(object, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert object.", e);
        }
    }

    public static <T> T convertValue(Object object, TypeReference<T> type) {
        try {
            return objectMapper.convertValue(object, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert object.", e);
        }
    }

    public static <T> T fromJsonString(String json, JavaType type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to convert object.", e);
        }
    }

}