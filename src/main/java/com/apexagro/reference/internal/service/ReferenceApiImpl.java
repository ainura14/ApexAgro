package com.apexagro.reference.internal.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apexagro.common.i18n.Language;
import com.apexagro.reference.api.ReferenceApi;
import com.apexagro.reference.api.dto.CategoryTreeDto;
import com.apexagro.reference.api.dto.CropDto;
import com.apexagro.reference.api.dto.CropUnitFactorDto;
import com.apexagro.reference.api.dto.SettlementDto;
import com.apexagro.reference.internal.entity.Category;
import com.apexagro.reference.internal.entity.Crop;
import com.apexagro.reference.internal.entity.CropUnitFactor;
import com.apexagro.reference.internal.entity.Settlement;
import com.apexagro.reference.internal.repository.CategoryRepository;
import com.apexagro.reference.internal.repository.CropRepository;
import com.apexagro.reference.internal.repository.CropUnitFactorRepository;
import com.apexagro.reference.internal.repository.SettlementRepository;

@Service
@Transactional(readOnly = true)
public class ReferenceApiImpl implements ReferenceApi {

    private final CropRepository cropRepository;
    private final CropUnitFactorRepository unitFactorRepository;
    private final SettlementRepository settlementRepository;
    private final CategoryRepository categoryRepository;

    public ReferenceApiImpl(
            CropRepository cropRepository,
            CropUnitFactorRepository unitFactorRepository,
            SettlementRepository settlementRepository,
            CategoryRepository categoryRepository
    ) {
        this.cropRepository = cropRepository;
        this.unitFactorRepository = unitFactorRepository;
        this.settlementRepository = settlementRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CropDto> getCrops(Language language) {
        return cropRepository.findAllByActiveTrueOrderByNameRuAsc().stream()
                .map(crop -> mapCropToDto(crop, language))
                .toList();
    }

    @Override
    public CropDto getCropById(UUID cropId, Language language) {
        return cropRepository.findById(cropId)
                .map(crop -> mapCropToDto(crop, language))
                .orElse(null);
    }

    @Override
    public List<SettlementDto> getSettlements(UUID districtId, Language language) {
        List<Settlement> settlements = settlementRepository.findByDistrictAndQuery(districtId, null);
        return settlements.stream()
                .map(s -> new SettlementDto(
                        s.getId(),
                        s.getDistrict().getId(),
                        s.getCode(),
                        language == Language.KY ? s.getNameKy() : s.getNameRu(),
                        s.getLat(),
                        s.getLon()
                ))
                .toList();
    }

    @Override
    public List<CategoryTreeDto> getCategoryTree(Language language) {
        List<Category> allCategories = categoryRepository.findAllByActiveTrueOrderBySortOrderAscNameRuAsc();
        Map<UUID, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return allCategories.stream()
                .filter(c -> c.getParent() == null)
                .map(root -> buildCategoryTreeDto(root, childrenMap, language))
                .toList();
    }

    private CropDto mapCropToDto(Crop crop, Language language) {
        String name = language == Language.KY ? crop.getNameKy() : crop.getNameRu();
        List<CropUnitFactor> factors = unitFactorRepository.findAllByCropId(crop.getId());
        List<CropUnitFactorDto> factorDtos = factors.stream()
                .map(f -> new CropUnitFactorDto(f.getUnit(), f.getKgPerUnit()))
                .toList();

        return new CropDto(crop.getId(), crop.getCode(), name, crop.isActive(), factorDtos);
    }

    private CategoryTreeDto buildCategoryTreeDto(Category category, Map<UUID, List<Category>> childrenMap, Language language) {
        String name = language == Language.KY ? category.getNameKy() : category.getNameRu();
        UUID parentId = category.getParent() != null ? category.getParent().getId() : null;

        List<Category> children = childrenMap.getOrDefault(category.getId(), Collections.emptyList());
        List<CategoryTreeDto> childDtos = children.stream()
                .map(child -> buildCategoryTreeDto(child, childrenMap, language))
                .toList();

        return new CategoryTreeDto(
                category.getId(),
                parentId,
                category.getLevel(),
                category.getCode(),
                name,
                category.getKind(),
                category.getSortOrder(),
                category.isActive(),
                childDtos
        );
    }
}