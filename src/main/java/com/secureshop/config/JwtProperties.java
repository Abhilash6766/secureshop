package com.secureshop.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties (
    String secret,
    long accessTokenExpirationMs,
    long refreshTokenExpirationMs
){}
