package com.example.guangzhouorder.config;

import com.example.guangzhouorder.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for stateless JWT
            .authorizeHttpRequests(auth -> auth

                    .requestMatchers("/api/chat/upload-image").authenticated()
                    .requestMatchers("/", "/signup", "/login", "/catalog", "/catalog/**", "/products", "/products/**", "/logout", "/error", "/verify-email",
                                 "/resend-verification", "/sourcing-service", "/logistics-hub", "/api-integration", "/terms", "/privacy", "/sourcing-guide", "/error", "/css/**", "/js/**", "/images/**", "/price-chart", "/api/price-chart/**").permitAll()

                .requestMatchers("/ws", "/ws/**").permitAll()
                .requestMatchers("/payment/webhook").permitAll()
                .requestMatchers("/customer/dashboard").hasRole("CUSTOMER")
                .requestMatchers("/affiliate/dashboard", "/affiliate/catalog").hasRole("AFFILIATE")
                .requestMatchers("/admin/dashboard").hasRole("ADMIN")
                .requestMatchers("/dashboard", "/orders", "/orders/**", "/chat", "/settings", "/settings/**").authenticated()

                .requestMatchers("/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendRedirect("/login"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.sendRedirect("/"))
            );

        return http.build();
    }
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
