package com.bazylev.server.models.tcp;

import com.bazylev.server.enums.RequestType;

public class Request {

    private RequestType requestType;
    private String token;
    private String data;

    public Request() {}

    public Request(RequestType requestType, String token, String data) {
        this.requestType = requestType;
        this.token = token;
        this.data = data;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
