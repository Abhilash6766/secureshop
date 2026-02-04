package com.secureshop.auth.controller;

import com.secureshop.audit.AuditedAction;
import com.secureshop.auth.dto.*;
import com.secureshop.auth.service.AuthService;
import com.secureshop.auth.util.DeviceId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        auth.register(req.email(), req.password());
        return ResponseEntity.ok().build();
    }

    @AuditedAction(action = "AUTH_LOGIN", entityType = "user")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String deviceId = DeviceId.require(request);
        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        return auth.login(req.email(), req.password(), deviceId, userAgent, ip);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest request) {
        String deviceId = DeviceId.require(request);
        return auth.refresh(req.refreshToken(), deviceId);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication, HttpServletRequest request) {
        String deviceId = DeviceId.require(request);
        String email = authentication.getName();
        Long userId = auth.getUserIdByEmail(email);
        auth.logout(userId, deviceId);
        return ResponseEntity.ok().build();
    }
}