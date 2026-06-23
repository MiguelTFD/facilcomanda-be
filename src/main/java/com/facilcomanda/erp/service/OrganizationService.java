package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.OrganizationRequest;
import com.facilcomanda.erp.dto.OrganizationResponse;
import com.facilcomanda.erp.model.Organization;
import com.facilcomanda.erp.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public OrganizationResponse createOrganization(OrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.name());
        organization.setTaxIdentificationNumber(request.taxIdentificationNumber());
        organization.setTaxIdentificationType(request.taxIdentificationType());

        Organization savedOrganization = organizationRepository.save(organization);
        return mapToResponse(savedOrganization);
    }

    public OrganizationResponse getOrganizationById(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return mapToResponse(organization);
    }

    public OrganizationResponse updateOrganization(Long id, OrganizationRequest request) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        organization.setName(request.name());
        organization.setTaxIdentificationNumber(request.taxIdentificationNumber());
        organization.setTaxIdentificationType(request.taxIdentificationType());

        Organization updatedOrganization = organizationRepository.save(organization);
        return mapToResponse(updatedOrganization);
    }

    private OrganizationResponse mapToResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getTaxIdentificationNumber(),
                organization.getTaxIdentificationType()
        );
    }
}
