package com.regionalai.floatingball.server.common.api;

import java.time.Instant;
import java.util.UUID;

public class ApiResponse<T> {

    private String code;
    private String message;
    private T data;
    private String requestId;
    private long timestamp;

    public ApiResponse() {
    }

    public ApiResponse(String code, String message, T data, String requestId, long timestamp) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, UUID.randomUUID().toString());
    }

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<T>("0", "success", data, requestId, Instant.now().toEpochMilli());
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<T>(code, message, null, requestId, Instant.now().toEpochMilli());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
