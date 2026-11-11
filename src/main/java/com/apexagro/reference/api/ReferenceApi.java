package com.apexagro.reference.api;

import java.util.List;
import java.util.UUID;

import com.apexagro.common.i18n.Language;
import com.apexagro.reference.api.dto.CategoryTreeDto;
import com.apexagro.reference.api.dto.CropDto;
import com.apexagro.reference.api.dto.SettlementDto;

public interface ReferenceApi {

    List<CropDto> getCrops(Language language);

    CropDto getCropById(UUID cropId, Language language);

    List<SettlementDto> getSettlements(UUID districtId, Language language);

    List<CategoryTreeDto> getCategoryTree(Language language);
}