package com.apexagro.reference.internal.dto.response;

import java.util.UUID;

public record DistrictResponse(
        UUID id,
        UUID regionId,
        String code,
        String name
) {
}