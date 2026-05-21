package com.smartacademic.config;

import com.smartacademic.entity.User;
import com.smartacademic.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class AppConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/auth/**", "/error").permitAll()
                        // VNPay redirect không kèm session JSESSIONID → cho phép public
                        .requestMatchers("/payment/vnpay-return").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/lecturer/**").hasRole("LECTURER")
                        .requestMatchers("/student/**", "/payment/**").hasRole("STUDENT")
                        .requestMatchers("/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/auth/access-denied")
                );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(12);
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return delegate.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword == null) return false;
                if (encodedPassword.startsWith("$2a$")
                        || encodedPassword.startsWith("$2b$")
                        || encodedPassword.startsWith("$2y$")) {
                    return delegate.matches(rawPassword, encodedPassword);
                }
                return rawPassword.toString().equals(encodedPassword);
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(UserRepository userRepository) {
        return new AuthenticationSuccessHandler() {
            @Override
            @Transactional(readOnly = true)
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                    User light = lightCopy(user);
                    request.getSession().setAttribute("currentUser", light);
                    request.getSession().setMaxInactiveInterval(30 * 60);
                });

                Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
                String contextPath = request.getContextPath();

                if (roles.contains("ROLE_ADMIN")) {
                    response.sendRedirect(contextPath + "/admin/dashboard");
                } else if (roles.contains("ROLE_LECTURER")) {
                    response.sendRedirect(contextPath + "/lecturer/dashboard");
                } else {
                    response.sendRedirect(contextPath + "/student/dashboard");
                }
            }

            // Tránh lưu entity có Lazy proxy vào session (gây LazyInitException khi view render).
            private User lightCopy(User src) {
                User u = new User();
                u.setId(src.getId());
                u.setUsername(src.getUsername());
                u.setEmail(src.getEmail());
                u.setRole(src.getRole());
                u.setIsActive(src.getIsActive());
                return u;
            }
        };
    }
}
