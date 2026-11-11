package com.apexagro.reference.internal.service;

import com.apexagro.common.i18n.Language;
import com.apexagro.reference.internal.dto.response.SettlementResponse;
import com.apexagro.reference.internal.entity.Settlement;
import com.apexagro.reference.internal.repository.SettlementRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public SettlementService(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    @Cacheable(cacheNames = "settlements", key = "{#districtId, #q, #language}")
    public List<SettlementResponse> getSettlements(UUID districtId, String q, Language language) {
        String cleanQuery = (q != null && !q.isBlank()) ? q.trim() : null;
        List<Settlement> settlements = settlementRepository.findByDistrictAndQuery(districtId, cleanQuery);

        return settlements.stream()
                .map(s -> new SettlementResponse(
                        s.getId(),
                        s.getDistrict().getId(),
                        s.getCode(),
                        language == Language.KY ? s.getNameKy() : s.getNameRu(),
                        s.getLat(),
                        s.getLon()
                ))
                .toList();
    }
}