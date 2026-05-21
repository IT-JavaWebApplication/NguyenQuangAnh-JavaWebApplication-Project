package com.smartacademic.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class AppConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép truy cập tự do vào các file tĩnh (CSS, JS, Hình ảnh)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()

                        // 2. Cho phép truy cập tự do vào các trang xác thực (Login, Register)
                        .requestMatchers("/auth/**").permitAll()

                        // 3. Các request khác yêu cầu phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .defaultSuccessUrl("/student/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .permitAll()
                );

        return http.build();
    }
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {

                // Lấy ra danh sách các Quyền (Roles) của user vừa đăng nhập thành công
                Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

                // Kiểm tra Role và điều hướng tương ứng
                if (roles.contains("ROLE_ADMIN")) {
                    response.sendRedirect("/admin/dashboard");
                } else if (roles.contains("ROLE_LECTURER")) {
                    response.sendRedirect("/lecturer/dashboard");
                } else if (roles.contains("ROLE_STUDENT")) {
                    response.sendRedirect("/student/dashboard");
                } else {
                    // Trang mặc định nếu không khớp Role nào
                    response.sendRedirect("/");
                }
            }
        };
    }
}