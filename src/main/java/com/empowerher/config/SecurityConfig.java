package com.empowerher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new com.empowerher.services.CustomUserDetailsService();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Custom Success Handler for role-based redirect - REMOVED DEBUG LOGS
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      org.springframework.security.core.Authentication authentication) 
                    throws IOException, ServletException {
                
                // Check user role and redirect accordingly
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                boolean isUser = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_USER"));
                
                if (isAdmin) {
                    response.sendRedirect("/admin/dashboard");
                } else if (isUser) {
                    response.sendRedirect("/user/dashboard");
                } else {
                    response.sendRedirect("/home");
                }
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Public routes - no authentication required
                .requestMatchers(
                    "/", 
                    "/home", 
                    "/scheme/**",
                   
                    "/categories", 
                    "/search",
                    "/register", 
                    "/login",
                    "/about",
                    "/contact",
                    "/access-denied",
                    "/css/**", 
                    "/js/**", 
                    "/images/**", 
                    "/uploads/**",
                    "/error",
                    "/test",
                    "/debug/**"
                ).permitAll()
                // Admin routes - require ADMIN role
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                // User routes - require USER role  
                .requestMatchers("/user/**").hasAuthority("ROLE_USER")
                // Public read-only API routes
                .requestMatchers(HttpMethod.GET,
                        "/api/schemes/**",
                        "/api/categories/**",
                        "/api/comments/**",
                        "/api/share/**").permitAll()
                // Administrative API mutations
                .requestMatchers(HttpMethod.POST, "/api/schemes/**", "/api/categories/**")
                        .hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/schemes/**", "/api/categories/**")
                        .hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/schemes/**", "/api/categories/**")
                        .hasAuthority("ROLE_ADMIN")
                // User-generated content and counters require an authenticated user
                .requestMatchers("/api/comments/**", "/api/share/**").authenticated()
                // All other routes require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler()) // Use custom success handler
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/access-denied")
            )
            .csrf(csrf -> {})
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}