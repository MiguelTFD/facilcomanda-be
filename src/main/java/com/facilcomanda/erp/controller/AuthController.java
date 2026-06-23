package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.LoginRequest;
import com.facilcomanda.erp.dto.LoginResponse;
import com.facilcomanda.erp.dto.RegisterRequest;
import com.facilcomanda.erp.dto.UserAuthDTO;
import com.facilcomanda.erp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @PostMapping("/register")
    public ResponseEntity<UserAuthDTO> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserAuthDTO newUser = authService.register(registerRequest);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }
}
