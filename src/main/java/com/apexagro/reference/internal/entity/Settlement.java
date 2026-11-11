package com.apexagro.reference.internal.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "settlements", schema = "reference")
public class Settlement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name_ky", nullable = false, length = 150)
    private String nameKy;

    @Column(name = "name_ru", nullable = false, length = 150)
    private String nameRu;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lon;

    protected Settlement() {
    }

    public UUID getId() {
        return id;
    }

    public District getDistrict() {
        return district;
    }

    public String getCode() {
        return code;
    }

    public String getNameKy() {
        return nameKy;
    }

    public String getNameRu() {
        return nameRu;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public BigDecimal getLon() {
        return lon;
    }
}