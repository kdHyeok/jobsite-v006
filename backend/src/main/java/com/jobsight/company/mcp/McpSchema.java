package com.jobsight.company.mcp;

import jakarta.validation.constraints.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

/** JSON schema for the record-based API DTOs, not a second handwritten API contract. */
final class McpSchema {
    private McpSchema() {}
    static Map<String, Object> of(Type type) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (type instanceof ParameterizedType list && list.getRawType() == List.class) {
            return Map.of("type", "array", "items", of(list.getActualTypeArguments()[0]));
        }
        Class<?> cls = (Class<?>) type;
        if (cls.isRecord()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (var component : cls.getRecordComponents()) {
                var schema = new LinkedHashMap<>(of(component.getGenericType()));
                try {
                    var field = cls.getDeclaredField(component.getName());
                    if (field.isAnnotationPresent(NotNull.class) || field.isAnnotationPresent(NotBlank.class)) required.add(component.getName());
                    else schema = new LinkedHashMap<>(Map.of("anyOf", List.of(schema, Map.of("type", "null"))));
                } catch (NoSuchFieldException impossible) { throw new IllegalStateException(impossible); }
                properties.put(component.getName(), schema);
            }
            return Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false);
        }
        if (cls == boolean.class || cls == Boolean.class) result.put("type", "boolean");
        else if (cls == int.class || cls == Integer.class || cls == Long.class) result.put("type", "integer");
        else {
            result.put("type", "string");
            if (cls.isEnum()) result.put("enum", Arrays.stream(cls.getEnumConstants()).map(Object::toString).toList());
            if (cls == UUID.class) result.put("format", "uuid");
            if (cls == Instant.class) result.put("format", "date-time");
            if (cls == LocalDate.class) result.put("format", "date");
        }
        return result;
    }

    static void validateShape(Object value, Type type) {
        if (value == null) return;
        if (type instanceof ParameterizedType list) {
            if (!(value instanceof List<?> values)) throw new IllegalArgumentException("Expected array");
            values.forEach(item -> {
                if (item == null) throw new IllegalArgumentException("Array entries cannot be null");
                validateShape(item, list.getActualTypeArguments()[0]);
            });
            return;
        }
        Class<?> cls = (Class<?>) type;
        if (cls.isRecord()) {
            if (!(value instanceof Map<?, ?> object)) throw new IllegalArgumentException("Expected object");
            var fields = Arrays.stream(cls.getRecordComponents()).map(RecordComponent::getName).toList();
            if (!fields.containsAll(object.keySet())) throw new IllegalArgumentException("Unknown request fields");
            for (var c : cls.getRecordComponents()) validateShape(object.get(c.getName()), c.getGenericType());
        } else if (cls == Boolean.class || cls == boolean.class) {
            if (!(value instanceof Boolean)) throw new IllegalArgumentException("Expected boolean");
        } else if (cls == Integer.class || cls == int.class || cls == Long.class) {
            if (!(value instanceof Number n) || n.doubleValue() != Math.rint(n.doubleValue())) throw new IllegalArgumentException("Expected integer");
        } else if (!(value instanceof String)) throw new IllegalArgumentException("Expected string");
    }
}
