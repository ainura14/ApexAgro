package com.apexagro.reference.internal.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.CropResponse;
import com.apexagro.reference.internal.entity.Crop;
import com.apexagro.reference.internal.repository.CropRepository;

@Service
@Transactional(readOnly = true)
public class CropService {

    private final CropRepository cropRepository;

    public CropService(CropRepository cropRepository) {
        this.cropRepository = cropRepository;
    }

    @Cacheable(cacheNames = "crops", key = "#language")
    public List<CropResponse> getCrops(Language language) {
        return cropRepository.findAllByActiveTrueOrderByNameRuAsc()
                .stream()
                .map(crop -> toResponse(crop, language))
                .toList();
    }

    private CropResponse toResponse(Crop crop, Language language) {
        String name = language == Language.KY
                ? crop.getNameKy()
                : crop.getNameRu();

        return new CropResponse(
                crop.getId(),
                crop.getCode(),
                name,
                crop.isActive()
        );
    }
}