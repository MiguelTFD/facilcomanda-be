package com.facilcomanda.erp.model;

import com.facilcomanda.erp.model.enums.RoleName;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.Column;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "roles", indexes = @Index(name = "idx_role_org", columnList = "organization_id"))
@Filter(name = "tenantFilter", condition = "organization_id = :organizationId")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    private RoleName name;

    private String description;

    public Role() {
    }

    public Role(Long organizationId, RoleName name, String description) {
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

    public RoleName getName() {
        return name;
    }

    public void setName(RoleName name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
