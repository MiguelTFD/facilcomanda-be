package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.UserRequest;
import com.facilcomanda.erp.dto.UserResponse;
import com.facilcomanda.erp.model.Role;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.repository.RoleRepository;
import com.facilcomanda.erp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserRequest request, Long organizationId) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required for user creation");
        }

        User user = new User();
        user.setOrganizationId(organizationId);
        user.setPassword(passwordEncoder.encode(request.password()));
        
        mapRequestToUser(request, user, organizationId);

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    public List<UserResponse> getAllUsers(Long organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id, Long organizationId) {
        User user = fetchUser(id, organizationId);
        return mapToResponse(user);
    }

    public UserResponse updateUser(Long id, UserRequest request, Long organizationId) {
        User user = fetchUser(id, organizationId);
        
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        
        mapRequestToUser(request, user, organizationId);

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    public void deleteUser(Long id, Long organizationId) {
        User user = fetchUser(id, organizationId);
        userRepository.delete(user);
    }

    private User fetchUser(Long id, Long organizationId) {
        return userRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("User not found or unauthorized"));
    }

    private void mapRequestToUser(UserRequest request, User user, Long organizationId) {
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        // SECURITY ENFORCEMENT: Never attach a role belonging to another tenant
        Role role = roleRepository.findByIdAndOrganizationId(request.roleId(), organizationId)
                .orElseThrow(() -> new RuntimeException("Role not found or unauthorized"));
        user.setRole(role);
    }

    private UserResponse mapToResponse(User user) {
        String roleName = user.getRole() != null ? user.getRole().getName().name() : null;
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roleName,
                user.getOrganizationId()
        );
    }
}
