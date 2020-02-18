package com.demo.client.feign;

import java.util.Locale;

public enum FeignProperties {

    TARGET_URL;

    private final String name;

    FeignProperties() {
        this.name = name().toLowerCase(Locale.ENGLISH);
    }

    public static String createPropertyKey(final Class<?> apiType, final String propertyName) {
        return apiType.getName().toLowerCase(Locale.ENGLISH) + '.' + propertyName;
    }

    public String getProperty(final Class<?> apiType) {
        return System.getProperty(createPropertyKey(apiType, name));
    }

    public String getProperty(final Class<?> apiType, final String def) {
        return System.getProperty(createPropertyKey(apiType, name), def);
    }

    public String setProperty(final Class<?> apiType, final String value) {
        return System.setProperty(createPropertyKey(apiType, name), value);
    }
}
