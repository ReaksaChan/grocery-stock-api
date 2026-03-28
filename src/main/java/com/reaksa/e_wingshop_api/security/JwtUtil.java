package com.reaksa.e_wingshop_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Token generation ──────────────────────────────────────────────

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return buildToken(userDetails.getUsername(), extraClaims, expirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), Map.of(), refreshExpirationMs);
    }

    private String buildToken(String subject, Map<String, Object> claims, long expiry) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token extraction ──────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public <T> T extractClaim(String token, String claimKey, Class<T> type) {
        return extractAllClaims(token).get(claimKey, type);
    }

    // ── Validation ────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // ── Internal ──────────────────────────────────────────────────────

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
