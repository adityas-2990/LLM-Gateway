package com.adisrivastava.gateway.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(ErrorDetail error) {
    public record ErrorDetail(
        String type,
        String message,
        String code,
        @JsonProperty("request_id") String requestId
    ) {}

    public static ErrorResponse of (String type , String message , String code , String requestId) {
        return new ErrorResponse(new ErrorDetail(type , message , code , requestId));
    }
}
