package com.apexagro.reference.internal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apexagro.common.i18n.ApexLocaleResolver;
import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.RegionResponse;
import com.apexagro.reference.internal.service.RegionService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionController {

    private final RegionService regionService;
    private final ApexLocaleResolver localeResolver;

    public RegionController(
            RegionService regionService,
            ApexLocaleResolver localeResolver
    ) {
        this.regionService = regionService;
        this.localeResolver = localeResolver;
    }

    @GetMapping
    public List<RegionResponse> getRegions(HttpServletRequest request) {
        Language language = localeResolver.resolveLanguage(request);
        return regionService.getRegions(language);
    }
}