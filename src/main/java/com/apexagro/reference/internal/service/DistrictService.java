package com.apexagro.reference.internal.service;

import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.DistrictResponse;
import com.apexagro.reference.internal.entity.District;
import com.apexagro.reference.internal.repository.DistrictRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DistrictService {

    private final DistrictRepository districtRepository;

    public DistrictService(DistrictRepository districtRepository) {
        this.districtRepository = districtRepository;
    }

    @Cacheable(cacheNames = "districts", key = "{#regionId, #language}")
    public List<DistrictResponse> getDistricts(UUID regionId, Language language) {
        List<District> districts = (regionId != null)
                ? districtRepository.findAllByRegionIdOrderByNameRuAsc(regionId)
                : districtRepository.findAllByOrderByNameRuAsc();

        return districts.stream()
                .map(district -> new DistrictResponse(
                        district.getId(),
                        district.getRegion().getId(),
                        district.getCode(),
                        language == Language.KY ? district.getNameKy() : district.getNameRu()
                ))
                .toList();
    }
}