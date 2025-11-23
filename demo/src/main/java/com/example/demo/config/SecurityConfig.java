package com.example.demo.config;

import com.example.demo.security.jwt.JwtAuthenticationEntryPoint;
import com.example.demo.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth-> auth
                        .requestMatchers("/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Cho phép xem sản phẩm công khai (GET requests)
                        .requestMatchers("GET", "/api/products/**").permitAll()
                        .requestMatchers("GET", "/api/categories/**").permitAll()
                        .requestMatchers("GET", "/api/brands/**").permitAll()
                        // Các endpoint tạo/sửa/xóa sản phẩm cần đăng nhập
                        .requestMatchers("/api/products/my-products/**").authenticated()
                        .requestMatchers("POST", "/api/products").authenticated()
                        .requestMatchers("PUT", "/api/products/**").authenticated()
                        .requestMatchers("DELETE", "/api/products/**").authenticated()
                        .requestMatchers("PATCH", "/api/products/**").authenticated()
                        // Cart và Order endpoints cần đăng nhập
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        // User endpoints
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/addresses/**").authenticated()
                        .anyRequest().authenticated()
                ).sessionManagement(sesion->sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                ) .authenticationProvider(authenticationProvider).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
    }

}
