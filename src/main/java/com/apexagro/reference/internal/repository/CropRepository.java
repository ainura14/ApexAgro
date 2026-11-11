package com.apexagro.reference.internal.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.apexagro.reference.internal.entity.Crop;

public interface CropRepository extends JpaRepository<Crop, UUID> {
    List<Crop> findAllByActiveTrueOrderByNameRuAsc();
}