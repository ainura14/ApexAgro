package com.apexagro.reference.api.dto;

import java.util.List;
import java.util.UUID;

public record CropDto(
        UUID id,
        String code,
        String name,
        boolean active,
        List<CropUnitFactorDto> unitFactors
) {
    public CropDto {
        unitFactors = List.copyOf(unitFactors);
    }
}