package com.example.demo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
    
    private SecretKey getSignInKey(){
        try {
            // Kiểm tra xem secret có phải là BASE64 không
            byte[] keyBytes;
            try {
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (IllegalArgumentException e) {
                // Nếu không phải BASE64, sử dụng trực tiếp string làm key
                // Lưu ý: Secret key nên được mã hóa BASE64 để đảm bảo độ dài phù hợp
                keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo JWT secret key: " + e.getMessage(), e);
        }
    }
}
