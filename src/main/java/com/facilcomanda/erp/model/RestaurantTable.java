package com.facilcomanda.erp.model;

import com.facilcomanda.erp.model.enums.TableState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "restaurant_tables", indexes = @Index(name = "idx_table_org", columnList = "organization_id"))
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "organizationId", type = Long.class))
@Filter(name = "tenantFilter", condition = "organization_id = :organizationId")
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;

    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TableState state;
    
    private Integer chairs;

    @ManyToOne
    @JoinColumn(name = "floor_id")
    private RestaurantFloor floor;

    public RestaurantTable() {}

    public RestaurantTable(Long organizationId, String name, String description, TableState state, Integer chairs) {
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
        this.state = state;
        this.chairs = chairs;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TableState getState() { return state; }
    public void setState(TableState state) { this.state = state; }
    public Integer getChairs() { return chairs; }
    public void setChairs(Integer chairs) { this.chairs = chairs; }
    public RestaurantFloor getFloor() { return floor; }
    public void setFloor(RestaurantFloor floor) { this.floor = floor; }
}
