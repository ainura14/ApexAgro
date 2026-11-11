package com.apexagro.reference.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementDto(
        UUID id,
        UUID districtId,
        String code,
        String name,
        BigDecimal lat,
        BigDecimal lon
) {
}