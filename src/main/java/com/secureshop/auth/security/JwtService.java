package com.secureshop.auth.security;

import com.secureshop.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props){
        this.props = props;

        if (props.secret() == null || props.secret().length() < 32){
            throw new IllegalArgumentException("security.jwt.secret must be atleast 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email) {
        return generateToken(userId, email, props.accessTokenExpirationMs(), "access");
    }

    public String generateRefreshToken(Long userId, String email) {
        return generateToken(userId, email, props.refreshTokenExpirationMs(), "refresh");
    }

    private String generateToken(Long userId, String email, long ttlMs, String type) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttlMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("uid", userId)
                .claim("typ", type)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public boolean isAccessToken(Jws<Claims> jws) {
        return "access".equals(jws.getBody().get("typ", String.class));
    }

    public boolean isRefreshToken(Jws<Claims> jws) {
        return "refresh".equals(jws.getBody().get("typ", String.class));
    }

    public Long getUserId(Jws<Claims> jws) {
        Object uid = jws.getBody().get("uid");
        if (uid instanceof Integer i) return i.longValue();
        if (uid instanceof Long l) return l;
        if (uid instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Invalid uid claim");
    }

    public Instant getExpiry(Jws<Claims> jws) {
        return jws.getBody().getExpiration().toInstant();
    }
}
