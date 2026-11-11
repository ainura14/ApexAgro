package com.apexagro.reference.internal.dto.response;

import java.util.UUID;

public record RegionResponse(
        UUID id,
        String code,
        String name
) {
}