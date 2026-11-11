package com.apexagro.reference.internal.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.apexagro.reference.internal.entity.CropUnitFactor;
import com.apexagro.reference.internal.entity.CropUnitFactorId;

public interface CropUnitFactorRepository
        extends JpaRepository<CropUnitFactor, CropUnitFactorId> {

    @Query("""
            SELECT factor
            FROM CropUnitFactor factor
            WHERE factor.id.cropId = :cropId
            ORDER BY factor.id.unit
            """)
    List<CropUnitFactor> findAllByCropId(@Param("cropId") UUID cropId);
}