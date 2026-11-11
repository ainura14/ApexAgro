package com.apexagro.reference.internal.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "crop_unit_factors", schema = "reference")
public class CropUnitFactor {

    @EmbeddedId
    private CropUnitFactorId id;

    @MapsId("cropId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @Column(name = "kg_per_unit", nullable = false, precision = 10, scale = 3)
    private BigDecimal kgPerUnit;

    protected CropUnitFactor() {
    }

    public CropUnitFactor(
            CropUnitFactorId id,
            Crop crop,
            BigDecimal kgPerUnit
    ) {
        this.id = id;
        this.crop = crop;
        this.kgPerUnit = kgPerUnit;
    }

    public CropUnitFactorId getId() {
        return id;
    }

    public Crop getCrop() {
        return crop;
    }

    public String getUnit() {
        return id.getUnit();
    }

    public BigDecimal getKgPerUnit() {
        return kgPerUnit;
    }
}