package com.bazylev.client.util;

import com.bazylev.client.enums.RequestType;
import com.bazylev.client.models.tcp.Request;
import com.bazylev.client.network.GsonFactory;
import com.google.gson.Gson;

public final class RequestBuilder {

    private static final Gson GSON = GsonFactory.getInstance();

    private RequestBuilder() {}

    public static Request of(RequestType type) {
        return new Request(type, null, null);
    }

    public static Request of(RequestType type, Object data) {
        return new Request(type, null, GSON.toJson(data));
    }

    public static Request ofRaw(RequestType type, String rawJson) {
        return new Request(type, null, rawJson);
    }
}
