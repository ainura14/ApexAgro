package com.apexagro.reference.internal.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.apexagro.reference.internal.entity.District;

public interface DistrictRepository extends JpaRepository<District, UUID> {

    List<District> findAllByOrderByNameRuAsc();

    List<District> findAllByRegionIdOrderByNameRuAsc(UUID regionId);
}