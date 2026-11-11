package com.apexagro.reference.api.dto;

import java.util.List;
import java.util.UUID;

public record CategoryTreeDto(
        UUID id,
        UUID parentId,
        short level,
        String code,
        String name,
        String kind,
        int sortOrder,
        boolean active,
        List<CategoryTreeDto> children
) {
    public CategoryTreeDto {
        children = List.copyOf(children);
    }
}