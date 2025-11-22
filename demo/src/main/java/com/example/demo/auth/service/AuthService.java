package com.example.demo.auth.service;

import com.example.demo.security.jwt.JwtService;
import com.example.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService  jwtService;
    private final JwtAuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

}
