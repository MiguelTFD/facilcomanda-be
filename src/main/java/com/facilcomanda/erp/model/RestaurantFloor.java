package com.facilcomanda.erp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Column;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "restaurant_floors", indexes = @Index(name = "idx_floor_org", columnList = "organization_id"))
@Filter(name = "tenantFilter", condition = "organization_id = :organizationId")
public class RestaurantFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    private String name;
    private String description;

    public RestaurantFloor() {}

    public RestaurantFloor(Long organizationId, String name, String description) {
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
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
}
