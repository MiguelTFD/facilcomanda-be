package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.RoleRequest;
import com.facilcomanda.erp.dto.RoleResponse;
import com.facilcomanda.erp.model.Role;
import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public RoleResponse createRole(RoleRequest request, Long organizationId) {
        Role role = new Role();
        role.setOrganizationId(organizationId);
        role.setName(RoleName.valueOf(request.name().toUpperCase()));
        role.setDescription(request.description());

        Role savedRole = roleRepository.save(role);
        return mapToResponse(savedRole);
    }

    public List<RoleResponse> getAllRoles(Long organizationId) {
        return roleRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getRoleById(Long id, Long organizationId) {
        Role role = fetchRole(id, organizationId);
        return mapToResponse(role);
    }

    public RoleResponse updateRole(Long id, RoleRequest request, Long organizationId) {
        Role role = fetchRole(id, organizationId);
        role.setName(RoleName.valueOf(request.name().toUpperCase()));
        role.setDescription(request.description());

        Role updatedRole = roleRepository.save(role);
        return mapToResponse(updatedRole);
    }

    public void deleteRole(Long id, Long organizationId) {
        Role role = fetchRole(id, organizationId);
        roleRepository.delete(role);
    }

    private Role fetchRole(Long id, Long organizationId) {
        return roleRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Role not found or unauthorized"));
    }

    private RoleResponse mapToResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName().name(),
                role.getDescription(),
                role.getOrganizationId()
        );
    }
}
