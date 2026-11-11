package com.apexagro.reference.internal.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.apexagro.reference.internal.entity.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    @Query("""
            SELECT s FROM Settlement s
            WHERE (:districtId IS NULL OR s.district.id = :districtId)
              AND (:q IS NULL OR LOWER(s.nameRu) LIKE LOWER(CONCAT('%', :q, '%')) 
                              OR LOWER(s.nameKy) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY s.nameRu ASC
            """)
    List<Settlement> findByDistrictAndQuery(@Param("districtId") UUID districtId, @Param("q") String q);
}