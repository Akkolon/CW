package com.bazylev.client.models.tcp;

import com.bazylev.client.enums.ResponseStatus;

public class Response {

    private ResponseStatus status;
    private String message;
    private String data;

    public Response() {}

    public Response(ResponseStatus status, String message, String data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static Response ok(String data) {
        return new Response(ResponseStatus.OK, "OK", data);
    }

    public static Response ok() {
        return new Response(ResponseStatus.OK, "OK", null);
    }

    public static Response error(String message) {
        return new Response(ResponseStatus.ERROR, message, null);
    }

    public static Response forbidden(String message) {
        return new Response(ResponseStatus.FORBIDDEN, message, null);
    }

    public static Response unauthorized() {
        return new Response(ResponseStatus.UNAUTHORIZED, "Не авторизован или токен истёк", null);
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
