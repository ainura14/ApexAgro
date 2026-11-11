package com.apexagro.reference.internal.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "districts", schema = "reference")
public class District {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name_ky", nullable = false, length = 100)
    private String nameKy;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    protected District() {
    }

    public UUID getId() {
        return id;
    }

    public Region getRegion() {
        return region;
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
}
