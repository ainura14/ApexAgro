package com.apexagro.reference.internal.service;

import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.RegionResponse;
import com.apexagro.reference.internal.repository.RegionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegionService {
    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Cacheable(
            cacheNames = "regions",
            key = "#language"
    )
    public List<RegionResponse> getRegions(Language language) {
        return regionRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(region -> new RegionResponse(
                        region.getId(),
                        region.getCode(),
                        language == Language.KY
                                ? region.getNameKy()
                                : region.getNameRu()
                ))
                .toList();
    }
}
