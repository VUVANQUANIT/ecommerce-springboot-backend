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
                .signWith(getSignInKey())
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
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    private SecretKey getSignInKey(){
        try {
            // Thử decode như Base64 trước
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            // Kiểm tra độ dài key (cần ít nhất 32 bytes = 256 bits)
            if (keyBytes.length < 32) {
                // Nếu key quá ngắn, hash nó để đảm bảo đủ 32 bytes
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                keyBytes = sha.digest(keyBytes);
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            // Nếu không phải Base64, sử dụng secret trực tiếp
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            // Đảm bảo key có ít nhất 32 bytes
            if (keyBytes.length < 32) {
                // Hash secret để có đủ 32 bytes
                try {
                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    keyBytes = sha.digest(keyBytes);
                } catch (Exception ex) {
                    throw new RuntimeException("Error creating JWT key", ex);
                }
            } else if (keyBytes.length > 64) {
                // Nếu key quá dài, chỉ lấy 64 bytes đầu (tối đa cho HS512)
                byte[] truncated = new byte[64];
                System.arraycopy(keyBytes, 0, truncated, 0, 64);
                keyBytes = truncated;
            }
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }
}
