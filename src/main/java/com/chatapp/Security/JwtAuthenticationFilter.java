package com.chatapp.Security;


import com.chatapp.service.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        log.debug("[LOGIN_DEBUG] JwtAuthenticationFilter.doFilterInternal() - Path: {}, HasAuthHeader: {}", 
                requestPath, authHeader != null);

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[LOGIN_DEBUG] No Bearer token found for path: {}, passing through filter chain", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        String userName;
        try {
            userName = jwtService.extractUserName(jwt);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[LOGIN_DEBUG] JWT extraction failed: {}", e.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if(userName != null && (SecurityContextHolder.getContext().getAuthentication()==null)){
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
            boolean tokenValid;
            try {
                tokenValid = jwtService.isTokenValid(jwt, userName);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("[LOGIN_DEBUG] JWT validation failed: {}", e.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if(tokenValid){
                Authentication auth = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
