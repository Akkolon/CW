package com.bazylev.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();
    private final Properties properties = new Properties();

    private AppConfig() {
        try (InputStream input = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("Файл config.properties не найден в classpath");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить config.properties", e);
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Параметр не найден в конфиге: " + key);
        }
        return value;
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public long getLong(String key) {
        return Long.parseLong(get(key));
    }
}
