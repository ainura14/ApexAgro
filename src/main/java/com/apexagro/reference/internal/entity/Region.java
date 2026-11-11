package com.apexagro.reference.internal.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "regions", schema = "reference")
public class Region {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name_ky", nullable = false, length = 100)
    private String nameKy;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Region() {
    }

    public UUID getId() {
        return id;
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

    public int getSortOrder() {
        return sortOrder;
    }
}
