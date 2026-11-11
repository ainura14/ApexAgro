package com.apexagro.reference.internal.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "crops", schema = "reference")
public class Crop {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name_ky", nullable = false, length = 100)
    private String nameKy;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    @Column(nullable = false)
    private boolean active;

    protected Crop() {
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

    public boolean isActive() {
        return active;
    }
}