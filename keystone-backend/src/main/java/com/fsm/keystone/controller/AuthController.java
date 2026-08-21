package com.fsm.keystone.controller;

import com.fsm.keystone.dto.AuthRequest;
import com.fsm.keystone.dto.AuthResponse;
import com.fsm.keystone.dto.CustomerOptionResponse;
import com.fsm.keystone.dto.SignupRequest;
import com.fsm.keystone.service.AuthService;
import com.fsm.keystone.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerService;

    @GetMapping("/customers")
    public List<CustomerOptionResponse> customers() {
        return customerService.listSignupOptions();
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request) {

        return ResponseEntity.ok(
                authService.signup(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}