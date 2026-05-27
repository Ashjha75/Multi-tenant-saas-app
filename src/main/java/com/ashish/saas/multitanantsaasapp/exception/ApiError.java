package com.ashish.saas.multitanantsaasapp.exception;

import java.time.Instant;
import java.util.Map;

public class ApiError {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String traceId;
    private final Map<String, Object> details;

    public ApiError(Instant timestamp, int status, String error, String message, String path, String traceId,
                    Map<String, Object> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getTraceId() {
        return traceId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

