package com.secureshop.auth.util;

import jakarta.servlet.http.HttpServletRequest;

public class DeviceId {
    public static final String HEADER = "X-Device-Id";

    public static String require(HttpServletRequest request) {
        String deviceId = request.getHeader(HEADER);
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Missing required header: " + HEADER);
        }
        if (deviceId.length() > 120) {
            throw new IllegalArgumentException("X-Device-Id too long");
        }
        return deviceId.trim();
    }
}