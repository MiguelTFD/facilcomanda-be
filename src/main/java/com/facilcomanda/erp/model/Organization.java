package com.facilcomanda.erp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String taxIdentificationType;
    private String taxIdentificationNumber;

    public Organization() {}

    public Organization(String name, String taxIdentificationType, String taxIdentificationNumber) {
        this.name = name;
        this.taxIdentificationType = taxIdentificationType;
        this.taxIdentificationNumber = taxIdentificationNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaxIdentificationType() { return taxIdentificationType; }
    public void setTaxIdentificationType(String taxIdentificationType) { this.taxIdentificationType = taxIdentificationType; }
    public String getTaxIdentificationNumber() { return taxIdentificationNumber; }
    public void setTaxIdentificationNumber(String taxIdentificationNumber) { this.taxIdentificationNumber = taxIdentificationNumber; }
}
