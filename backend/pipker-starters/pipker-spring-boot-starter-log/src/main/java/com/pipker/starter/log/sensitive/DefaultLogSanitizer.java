/**
 * @file DefaultLogSanitizer.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 对常见容器、Record 和 JavaBean 生成脱敏日志副本的默认实现。
 * @logic 按字段名和 @Sensitive 解析脱敏规则，递归复制可读值并防护循环引用、异常 Getter 与未知对象。
 * @dependencies PipkerLogProperties、Sensitive、Java Beans Introspector
 * @index_tags log、sensitive、masking
 * @author holic512
 */
package com.pipker.starter.log.sensitive;

import com.pipker.starter.log.annotation.Sensitive;
import com.pipker.starter.log.config.PipkerLogProperties;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DefaultLogSanitizer implements LogSanitizer {

    private static final String UNAVAILABLE = "<unavailable>";
    private static final String CIRCULAR_REFERENCE = "<circular-reference>";
    private static final int MAX_DEPTH = 8;

    private static final Map<String, SensitiveType> DEFAULT_FIELDS = Map.ofEntries(
            Map.entry("password", SensitiveType.PASSWORD),
            Map.entry("passwd", SensitiveType.PASSWORD),
            Map.entry("pwd", SensitiveType.PASSWORD),
            Map.entry("token", SensitiveType.TOKEN),
            Map.entry("accesstoken", SensitiveType.TOKEN),
            Map.entry("refreshtoken", SensitiveType.TOKEN),
            Map.entry("authorization", SensitiveType.TOKEN),
            Map.entry("cookie", SensitiveType.TOKEN),
            Map.entry("phone", SensitiveType.PHONE),
            Map.entry("mobile", SensitiveType.PHONE),
            Map.entry("telephone", SensitiveType.PHONE),
            Map.entry("email", SensitiveType.EMAIL),
            Map.entry("idcard", SensitiveType.ID_CARD),
            Map.entry("identitycard", SensitiveType.ID_CARD),
            Map.entry("bankcard", SensitiveType.BANK_CARD),
            Map.entry("cardnumber", SensitiveType.BANK_CARD),
            Map.entry("cardno", SensitiveType.BANK_CARD)
    );

    private final PipkerLogProperties properties;

    public DefaultLogSanitizer(PipkerLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object sanitize(Object value) {
        return sanitize(value, null);
    }

    @Override
    public Object sanitize(Object value, SensitiveType explicitType) {
        try {
            return sanitize(value, explicitType, new IdentityHashMap<>(), 0);
        } catch (RuntimeException exception) {
            return UNAVAILABLE;
        }
    }

    private Object sanitize(
            Object value,
            SensitiveType explicitType,
            IdentityHashMap<Object, Boolean> visited,
            int depth
    ) {
        if (value == null) {
            return null;
        }
        if (explicitType != null && properties.getSensitive().isEnabled()) {
            return mask(value, explicitType);
        }
        if (depth > MAX_DEPTH) {
            return "<max-depth>";
        }
        if (isSimpleValue(value)) {
            return value instanceof CharSequence || value instanceof Character ? value.toString() : value;
        }
        if (visited.put(value, Boolean.TRUE) != null) {
            return CIRCULAR_REFERENCE;
        }
        try {
            if (value instanceof Map<?, ?> map) {
                return sanitizeMap(map, visited, depth);
            }
            if (value instanceof Collection<?> collection) {
                return sanitizeCollection(collection, visited, depth);
            }
            if (value.getClass().isArray()) {
                return sanitizeArray(value, visited, depth);
            }
            if (value.getClass().isRecord()) {
                return sanitizeRecord(value, visited, depth);
            }
            if (value.getClass().getPackageName().startsWith("java.")) {
                return safeToString(value);
            }
            return sanitizeBean(value, visited, depth);
        } finally {
            visited.remove(value);
        }
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> map, IdentityHashMap<Object, Boolean> visited, int depth) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            String fieldName = String.valueOf(key);
            sanitized.put(fieldName, sanitize(value, sensitiveTypeFor(fieldName), visited, depth + 1));
        });
        return sanitized;
    }

    private Collection<Object> sanitizeCollection(
            Collection<?> collection,
            IdentityHashMap<Object, Boolean> visited,
            int depth
    ) {
        Collection<Object> sanitized = new ArrayList<>(collection.size());
        for (Object element : collection) {
            sanitized.add(sanitize(element, null, visited, depth + 1));
        }
        return sanitized;
    }

    private Collection<Object> sanitizeArray(Object array, IdentityHashMap<Object, Boolean> visited, int depth) {
        int length = Array.getLength(array);
        Collection<Object> sanitized = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            sanitized.add(sanitize(Array.get(array, index), null, visited, depth + 1));
        }
        return sanitized;
    }

    private Map<String, Object> sanitizeRecord(Object record, IdentityHashMap<Object, Boolean> visited, int depth) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            Sensitive annotation = component.getAnnotation(Sensitive.class);
            try {
                Method accessor = component.getAccessor();
                if (!accessor.canAccess(record) && !accessor.trySetAccessible()) {
                    sanitized.put(component.getName(), UNAVAILABLE);
                    continue;
                }
                Object componentValue = accessor.invoke(record);
                sanitized.put(
                        component.getName(),
                        sanitize(componentValue, annotation == null ? sensitiveTypeFor(component.getName()) : annotation.value(), visited, depth + 1)
                );
            } catch (IllegalAccessException | InvocationTargetException exception) {
                sanitized.put(component.getName(), UNAVAILABLE);
            }
        }
        return sanitized;
    }

    private Map<String, Object> sanitizeBean(Object bean, IdentityHashMap<Object, Boolean> visited, int depth) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        try {
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors()) {
                Method readMethod = descriptor.getReadMethod();
                if (readMethod == null || readMethod.getParameterCount() != 0) {
                    continue;
                }
                Sensitive annotation = sensitiveAnnotation(bean.getClass(), descriptor.getName(), readMethod);
                try {
                    if (!readMethod.canAccess(bean) && !readMethod.trySetAccessible()) {
                        sanitized.put(descriptor.getName(), UNAVAILABLE);
                        continue;
                    }
                    Object propertyValue = readMethod.invoke(bean);
                    sanitized.put(
                            descriptor.getName(),
                            sanitize(propertyValue, annotation == null ? sensitiveTypeFor(descriptor.getName()) : annotation.value(), visited, depth + 1)
                    );
                } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
                    sanitized.put(descriptor.getName(), UNAVAILABLE);
                }
            }
        } catch (IntrospectionException exception) {
            return Map.of("value", UNAVAILABLE);
        }
        return sanitized.isEmpty() ? Map.of("value", safeToString(bean)) : sanitized;
    }

    private Sensitive sensitiveAnnotation(Class<?> type, String fieldName, Method readMethod) {
        Sensitive methodAnnotation = readMethod.getAnnotation(Sensitive.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                return field.getAnnotation(Sensitive.class);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private SensitiveType sensitiveTypeFor(String fieldName) {
        if (!properties.getSensitive().isEnabled() || fieldName == null) {
            return null;
        }
        String normalized = normalizeFieldName(fieldName);
        SensitiveType defaultType = DEFAULT_FIELDS.get(normalized);
        if (defaultType != null) {
            return defaultType;
        }
        return properties.getSensitive().getAdditionalFields().entrySet().stream()
                .filter(entry -> normalizeFieldName(entry.getKey()).equals(normalized))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String normalizeFieldName(String fieldName) {
        return fieldName.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Character
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof TemporalAccessor;
    }

    private Object mask(Object value, SensitiveType type) {
        String text = safeToString(value);
        String mask = properties.getSensitive().getMaskText();
        if (mask == null || mask.isBlank()) {
            mask = "******";
        }
        return switch (type) {
            case PASSWORD, TOKEN -> mask;
            case PHONE -> maskMiddle(text, 3, 4, mask);
            case EMAIL -> maskEmail(text, mask);
            case ID_CARD -> maskMiddle(text, 6, 4, mask);
            case BANK_CARD -> maskMiddle(text, 4, 4, mask);
        };
    }

    private String maskMiddle(String text, int prefixLength, int suffixLength, String mask) {
        if (text.length() <= prefixLength + suffixLength) {
            return mask;
        }
        return text.substring(0, prefixLength) + mask + text.substring(text.length() - suffixLength);
    }

    private String maskEmail(String text, String mask) {
        int atIndex = text.indexOf('@');
        if (atIndex <= 0 || atIndex == text.length() - 1) {
            return mask;
        }
        int visibleLength = Math.min(2, atIndex);
        return text.substring(0, visibleLength) + mask + text.substring(atIndex);
    }

    private String safeToString(Object value) {
        try {
            return String.valueOf(value);
        } catch (RuntimeException exception) {
            return UNAVAILABLE;
        }
    }
}
