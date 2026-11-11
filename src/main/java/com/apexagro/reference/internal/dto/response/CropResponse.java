package com.apexagro.reference.internal.dto.response;

import java.util.UUID;

public record CropResponse(
        UUID id,
        String code,
        String name,
        boolean active
) {
}
