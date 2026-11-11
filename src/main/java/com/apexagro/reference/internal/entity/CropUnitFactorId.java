package com.apexagro.reference.internal.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CropUnitFactorId implements Serializable {

    @Column(name = "crop_id", nullable = false)
    private UUID cropId;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit;

    protected CropUnitFactorId() {
    }

    public CropUnitFactorId(UUID cropId, String unit) {
        this.cropId = cropId;
        this.unit = unit;
    }

    public UUID getCropId() {
        return cropId;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CropUnitFactorId that)) {
            return false;
        }
        return Objects.equals(cropId, that.cropId)
               && Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cropId, unit);
    }
}