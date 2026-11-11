package com.apexagro.reference.api.dto;

import java.math.BigDecimal;

public record CropUnitFactorDto(
        String unit,
        BigDecimal kgPerUnit
) {
}