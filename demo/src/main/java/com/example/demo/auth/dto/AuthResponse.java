package com.example.demo.auth.dto;

import com.example.demo.user.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String fullname;
    private Role role;
}
