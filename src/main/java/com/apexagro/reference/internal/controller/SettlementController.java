package com.apexagro.reference.internal.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apexagro.common.i18n.ApexLocaleResolver;
import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.SettlementResponse;
import com.apexagro.reference.internal.service.SettlementService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final ApexLocaleResolver localeResolver;

    public SettlementController(SettlementService settlementService, ApexLocaleResolver localeResolver) {
        this.settlementService = settlementService;
        this.localeResolver = localeResolver;
    }

    @GetMapping
    public List<SettlementResponse> getSettlements(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) String q,
            HttpServletRequest request
    ) {
        Language language = localeResolver.resolveLanguage(request);
        return settlementService.getSettlements(districtId, q, language);
    }
}