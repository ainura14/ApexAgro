package com.apexagro.reference.internal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apexagro.common.i18n.ApexLocaleResolver;
import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.CropResponse;
import com.apexagro.reference.internal.service.CropService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/crops")
public class CropController {

    private final CropService cropService;
    private final ApexLocaleResolver localeResolver;

    public CropController(
            CropService cropService,
            ApexLocaleResolver localeResolver
    ) {
        this.cropService = cropService;
        this.localeResolver = localeResolver;
    }

    @GetMapping
    public List<CropResponse> getCrops(HttpServletRequest request) {
        Language language = localeResolver.resolveLanguage(request);
        return cropService.getCrops(language);
    }
}