package com.apexagro.reference.internal.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID districtId,
        String code,
        String name,
        BigDecimal lat,
        BigDecimal lon
) {
}