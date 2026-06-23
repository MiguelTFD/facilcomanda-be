package com.facilcomanda.erp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Column;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "categories", indexes = @Index(name = "idx_category_org", columnList = "organization_id"))
@Filter(name = "tenantFilter", condition = "organization_id = :organizationId")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    private String name;

    private String description;

    @ManyToOne
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    public Category() {
    }

    public Category(Long organizationId, String name, String description, Category parentCategory) {
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
        this.parentCategory = parentCategory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getParentCategory() {
        return parentCategory;
    }

    public void setParentCategory(Category parentCategory) {
        this.parentCategory = parentCategory;
    }
}
