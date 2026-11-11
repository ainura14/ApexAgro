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
@Table(name = "categories", schema = "reference")
public class Category {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private short level;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name_ky", nullable = false, length = 100)
    private String nameKy;

    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    protected Category() {
    }

    public UUID getId() {
        return id;
    }

    public Category getParent() {
        return parent;
    }

    public short getLevel() {
        return level;
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

    public String getKind() {
        return kind;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}