package com.apexagro.identity.internal.dto.request;

public record VerifyOtpRequest(
        String phone,
        String code,
        String device
) {
}