package com.example.demo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims,userDetails.getUsername(),accessTokenExpiration);
    }
    public String generateRefreshToken(UserDetails userDetails) {
        return createToken(new HashMap<>(),userDetails.getUsername(),refreshTokenExpiration);
    }
    public String createToken(Map<String, Object> claims,String subject,long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(getSignInKey(),SignatureAlgorithm.HS256)
                .compact();
    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.security.SecurityException | io.jsonwebtoken.MalformedJwtException e) {
            throw new RuntimeException("JWT token không hợp lệ: " + e.getMessage(), e);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("JWT token đã hết hạn", e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý JWT token: " + e.getMessage(), e);
        }
    }

    private SecretKey getSignInKey() {
        try {
            byte[] keyBytes;
            if (isBase64(secret)) {
                keyBytes = Decoders.BASE64.decode(secret);
            } else {
                keyBytes = createSecureKeyFromString(secret);
            }
            if (keyBytes.length < 32)
            {
                logger.warn("JWT secret key might be too weak. Recommended length: 32 bytes (256-bit)");
            }

            return Keys.hmacShaKeyFor(keyBytes);

        } catch (Exception e) {
            logger.error("Failed to initialize JWT secret key: {}", e.getMessage());
            throw new RuntimeException("JWT secret key configuration error", e);
        }
    }

    private boolean isBase64(String value) {
        try {
            Decoders.BASE64.decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] createSecureKeyFromString(String secret) {
        try {
            // Sử dụng SHA-256 để tạo key 32 bytes cố định từ string
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create secure key from string", e);
        }
    }
}
