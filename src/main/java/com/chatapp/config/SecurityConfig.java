package com.chatapp.config;

import com.chatapp.Security.JwtAuthenticationFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AllArgsConstructor
@Configuration
@Slf4j
public class SecurityConfig {

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        log.info("[LOGIN_DEBUG] SecurityConfig.securityFilterChain() - Setting up security filter chain");
        http
                .authenticationProvider(daoAuthenticationProvider)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    log.info("[LOGIN_DEBUG] Configuring authorized requests - /auth/** is permitAll");
                    auth.requestMatchers(
                            "/index.html",
                            "/",
                            "/ws/**",
                            "/login.html",
                            "/register.html",
                            "/app.js",
                            "/style.css",
                            "/auth/**",
                            "/favicon.ico",
                            "/relay-main.png"
                    ).permitAll()
                    .anyRequest().authenticated();
                })
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        log.info("[LOGIN_DEBUG] SecurityConfig.securityFilterChain() - Filter chain setup complete");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.info("[LOGIN_DEBUG] SecurityConfig.authenticationManager() - Creating AuthenticationManager");
        AuthenticationManager manager = config.getAuthenticationManager();
        log.info("[LOGIN_DEBUG] SecurityConfig.authenticationManager() - AuthenticationManager created successfully");
        return manager;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }
}
