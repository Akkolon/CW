package com.bazylev.server.network;

import com.bazylev.server.enums.RequestType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class GsonFactory {

    private static final Gson INSTANCE = buildGson();

    private GsonFactory() {}

    public static Gson getInstance() {
        return INSTANCE;
    }

    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, type, ctx) ->
                                new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, type, ctx) ->
                                LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .registerTypeAdapter(LocalTime.class,
                        (JsonSerializer<LocalTime>) (src, type, ctx) ->
                                new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME)))
                .registerTypeAdapter(LocalTime.class,
                        (JsonDeserializer<LocalTime>) (json, type, ctx) ->
                                LocalTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_TIME))
                .registerTypeAdapter(RequestType.class,
                        (JsonDeserializer<RequestType>) (json, type, ctx) -> {
                            if (json == null || json.isJsonNull()) return null;
                            String raw = json.getAsString();
                            if (raw == null) return null;
                            String normalized = raw.trim()
                                    .replace('-', '_')
                                    .toUpperCase();
                            if (normalized.isBlank()) return null;
                            try {
                                return RequestType.valueOf(normalized);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                .registerTypeAdapter(RequestType.class,
                        (JsonSerializer<RequestType>) (src, type, ctx) ->
                                src == null ? null : new JsonPrimitive(src.name()))
                .create();
    }
}
