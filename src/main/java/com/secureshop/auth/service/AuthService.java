package com.secureshop.auth.service;

import com.secureshop.auth.domain.Session;
import com.secureshop.auth.dto.AuthResponse;
import com.secureshop.auth.repo.RoleRepository;
import com.secureshop.auth.security.JwtService;
import com.secureshop.auth.security.TokenHash;
import com.secureshop.auth.repo.SessionRepository;
import com.secureshop.user.domain.User;
import com.secureshop.user.repo.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository users;
    private final SessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final TokenHash tokenHash;
    private final RoleRepository roles;

    public AuthService(UserRepository users,
                       SessionRepository sessions,
                       PasswordEncoder encoder,
                       JwtService jwtService,
                       TokenHash tokenHash, RoleRepository roles) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.tokenHash = tokenHash;
        this.roles = roles;
    }

    public User register(String email, String rawPassword) {
        String normalized = email.toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw new IllegalArgumentException("Email already in use");
        }
        var customerRole = roles.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role not seeded"));

        var user = User.builder()
                .email(normalized)
                .passwordHash(encoder.encode(rawPassword))
                .status("ACTIVE")
                .build();

        user.getRoles().add(customerRole);
        return users.save(user);
    }


    public AuthResponse login(String email, String rawPassword, String deviceId, String userAgent, String ip) {
        var user = users.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail());


        String refreshHash = tokenHash.sha256(refresh);


        Jws<Claims> refreshJws = jwtService.parse(refresh);
        Instant refreshExp = jwtService.getExpiry(refreshJws);

        String uaHash = (userAgent == null || userAgent.isBlank()) ? null : tokenHash.sha256(userAgent);
        String ipHash = (ip == null || ip.isBlank()) ? null : tokenHash.sha256(ip);

        Session session = sessions.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElse(Session.builder()
                        .userId(user.getId())
                        .deviceId(deviceId)
                        .build());

        session.setRefreshTokenHash(refreshHash);
        session.setExpiresAt(refreshExp);
        session.setUserAgentHash(uaHash);
        session.setIpHash(ipHash);
        session.setRevokedAt(null);
        session.setRotatedAt(null);

        sessions.save(session);

        return new AuthResponse(access, refresh, "Bearer", 900);
    }


    public AuthResponse refresh(String refreshToken, String deviceId) {
        Jws<Claims> jws = jwtService.parse(refreshToken);

        if (!jwtService.isRefreshToken(jws)) {
            throw new IllegalArgumentException("Not a refresh token");
        }

        Long userId = jwtService.getUserId(jws);
        String email = jws.getBody().getSubject();
        Instant exp = jwtService.getExpiry(jws);

        Session session = sessions.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.isActive()) {
            throw new IllegalArgumentException("Session is revoked or expired");
        }

        if (Instant.now().isAfter(exp)) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        String providedHash = tokenHash.sha256(refreshToken);


        if (!providedHash.equals(session.getRefreshTokenHash())) {
            session.setRevokedAt(Instant.now());
            sessions.save(session);
            throw new IllegalArgumentException("Refresh token reuse detected. Session revoked.");
        }


        String newAccess = jwtService.generateAccessToken(userId, email);
        String newRefresh = jwtService.generateRefreshToken(userId, email);

        String newRefreshHash = tokenHash.sha256(newRefresh);
        Instant newRefreshExp = jwtService.getExpiry(jwtService.parse(newRefresh));

        session.setRefreshTokenHash(newRefreshHash);
        session.setExpiresAt(newRefreshExp);
        session.setRotatedAt(Instant.now());

        sessions.save(session);

        return new AuthResponse(newAccess, newRefresh, "Bearer", 900);
    }

    public void logout(Long userId, String deviceId) {
        Session session = sessions.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setRevokedAt(Instant.now());
        sessions.save(session);
    }

    public Long getUserIdByEmail(String email) {
        return users.findByEmail(email.toLowerCase())
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}