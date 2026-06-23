package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.UserRequest;
import com.facilcomanda.erp.dto.UserResponse;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request, Authentication authentication) {
        return new ResponseEntity<>(userService.createUser(request, getOrganizationId(authentication)), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        return ResponseEntity.ok(userService.getAllUsers(getOrganizationId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(userService.getUserById(id, getOrganizationId(authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request, Authentication authentication) {
        return ResponseEntity.ok(userService.updateUser(id, request, getOrganizationId(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id, getOrganizationId(authentication));
        return ResponseEntity.noContent().build();
    }
}
