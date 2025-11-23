package com.example.demo.auth.service;

import com.example.demo.auth.dto.AuthResponse;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.security.jwt.JwtService;
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService  jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";


    public AuthResponse register(RegisterRequest registerRequest) {
        var user = User.builder()
                .fullName(registerRequest.getFullname())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phone(registerRequest.getPhone())
                .role(registerRequest.getRole()==null ? Role.USER : registerRequest.getRole()).build();
        userRepository.save(user);
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullname(user.getFullName())
                .role(user.getRole())
                .build();
    }
    public AuthResponse refreshtoken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        var user = userRepository.findUserByEmail(email).orElseThrow();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            throw new RuntimeException("Refesh token đã bị thu hồi");
        }
        if (!jwtService.isTokenValid(refreshToken,user)){
            throw new RuntimeException("Refesh token không hợp lệ");
        }
        var newAccessToken = jwtService.generateToken(user);
        var newRefreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).fullname(user.getFullName()).role(user.getRole()).build();

    }
    public void logout(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        if (userRepository.findUserByEmail(email).isPresent()) {
            long expiration = jwtService.extractExpiration(refreshToken).getTime()-System.currentTimeMillis();
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + refreshToken, "blacklisted");
        }
    }
    public AuthResponse login(LoginRequest loginRequest) {
               authenticationManager.authenticate(
                       new UsernamePasswordAuthenticationToken(
                               loginRequest.getEmail(),loginRequest.getPassword()
                       )
               );
               var user = userRepository.findUserByEmail(loginRequest.getEmail()).orElseThrow();
               var accessToken = jwtService.generateToken(user);
               var refreshToken = jwtService.generateRefreshToken(user);
               return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken).fullname(user.getFullName()).role(user.getRole()).build();
    }

}
