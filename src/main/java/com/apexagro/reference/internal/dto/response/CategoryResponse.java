package com.apexagro.reference.internal.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        short level,
        String code,
        String name,
        String kind,
        int sortOrder,
        boolean active,
        List<CategoryResponse> children
) {
}