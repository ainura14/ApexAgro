package com.apexagro.reference.internal.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apexagro.common.i18n.ApexLocaleResolver;
import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.DistrictResponse;
import com.apexagro.reference.internal.service.DistrictService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/districts")
public class DistrictController {

    private final DistrictService districtService;
    private final ApexLocaleResolver localeResolver;

    public DistrictController(DistrictService districtService, ApexLocaleResolver localeResolver) {
        this.districtService = districtService;
        this.localeResolver = localeResolver;
    }

    @GetMapping
    public List<DistrictResponse> getDistricts(
            @RequestParam(required = false) UUID regionId,
            HttpServletRequest request
    ) {
        Language language = localeResolver.resolveLanguage(request);
        return districtService.getDistricts(regionId, language);
    }
}