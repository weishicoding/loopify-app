package com.loopify.mainservice.config;

import com.loopify.mainservice.security.TrustedHeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {


    private final TrustedHeaderAuthenticationFilter trustedHeaderFilter;
//    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // Stateless API, no CSRF needed
                .httpBasic(AbstractHttpConfigurer::disable) // <-- Disable HTTP Basic
                .formLogin(AbstractHttpConfigurer::disable) // <-- Disable Form Login
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()  // Public endpoints
                        .anyRequest().authenticated())  // Everything else requires auth
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                //.authenticationProvider(authenticationProvider())
                .addFilterBefore(trustedHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
