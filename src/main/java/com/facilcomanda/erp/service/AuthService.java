package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.LoginRequest;
import com.facilcomanda.erp.dto.LoginResponse;
import com.facilcomanda.erp.dto.RegisterRequest;
import com.facilcomanda.erp.dto.UserAuthDTO;
import com.facilcomanda.erp.model.Organization;
import com.facilcomanda.erp.model.Role;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.repository.OrganizationRepository;
import com.facilcomanda.erp.repository.RoleRepository;
import com.facilcomanda.erp.repository.UserRepository;
import com.facilcomanda.erp.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, 
                       RoleRepository roleRepository, 
                       OrganizationRepository organizationRepository, 
                       JwtService jwtService, 
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAuthDTO register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("User with this email already exists.");
        }

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found."));

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new RuntimeException("Role not found."));

        User user = new User();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setOrganization(organization);
        user.setOrganizationId(organization.getId());
        user.setRole(role);
        
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        return new UserAuthDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role.getName(),
                organization.getId()
        );
    }

    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        String jwtToken = jwtService.generateToken(user);

        UserAuthDTO userDto = new UserAuthDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName(),
                user.getOrganizationId()
        );

        return new LoginResponse(jwtToken, userDto);
    }
}
