package com.chatapp.Security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey(){
        if (secret == null || secret.isEmpty()) {
            log.error("[LOGIN_DEBUG] JWT_SECRET is not set or is empty!");
            throw new IllegalArgumentException("JWT_SECRET must be configured");
        }
        log.debug("[LOGIN_DEBUG] JWT_SECRET is set, length: {}", secret.length());
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            log.debug("[LOGIN_DEBUG] SigningKey created successfully");
            return key;
        } catch (Exception e) {
            log.error("[LOGIN_DEBUG] Failed to create SigningKey - Exception: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    public String generateToken(String userName){
        log.debug("[LOGIN_DEBUG] JwtService.generateToken() called for username: {}", userName);
        try {
            String token = Jwts.builder()
                    .subject(userName)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(getSigningKey())
                    .compact();
            log.debug("[LOGIN_DEBUG] JWT token generated successfully for username: {}", userName);
            return token;
        } catch (Exception e) {
            log.error("[LOGIN_DEBUG] JWT generation failed - Exception: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    public String extractUserName(String token){
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }


   private boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
   }

   public boolean isTokenValid(String token, String userName){
        final String extractedUserName = extractUserName(token);

        return extractedUserName.equals(userName) && !isTokenExpired(token);
   }


}
