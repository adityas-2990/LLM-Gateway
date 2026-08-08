package com.adisrivastava.gateway.common;

import org.springframework.http.HttpStatus;

public class GatewayException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String type;

    public GatewayException(HttpStatus status, String code, String type, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.type = type;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
