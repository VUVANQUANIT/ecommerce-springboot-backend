package com.example.demo.auth.dto;

import com.example.demo.user.entity.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String fullname;
    private String email;
    private String password;
    private Role role;
}
