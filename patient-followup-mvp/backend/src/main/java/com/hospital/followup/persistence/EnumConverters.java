package com.hospital.followup.persistence;

import org.springframework.util.StringUtils;

public final class EnumConverters {

    private EnumConverters() {
    }

    public static <T extends Enum<T>> T read(Class<T> enumType, String databaseValue) {
        if (!StringUtils.hasText(databaseValue)) {
            return null;
        }
        return Enum.valueOf(enumType, databaseValue.trim().toUpperCase());
    }

    public static <T extends Enum<T>> String write(T value) {
        return value == null ? null : value.name();
    }
}
