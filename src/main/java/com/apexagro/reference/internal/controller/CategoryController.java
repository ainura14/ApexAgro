package com.apexagro.reference.internal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apexagro.common.i18n.ApexLocaleResolver;
import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.CategoryResponse;
import com.apexagro.reference.internal.service.CategoryService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ApexLocaleResolver localeResolver;

    public CategoryController(
            CategoryService categoryService,
            ApexLocaleResolver localeResolver
    ) {
        this.categoryService = categoryService;
        this.localeResolver = localeResolver;
    }

    @GetMapping
    public List<CategoryResponse> getCategories(HttpServletRequest request) {
        Language language = localeResolver.resolveLanguage(request);
        return categoryService.getCategories(language);
    }
}