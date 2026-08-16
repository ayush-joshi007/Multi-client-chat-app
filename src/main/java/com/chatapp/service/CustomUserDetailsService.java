package com.chatapp.service;

import com.chatapp.entity.UserEntity;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.UserDetailsManagerConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("[LOGIN_DEBUG] loadUserByUsername called for username: {}", username);
        
        Optional<UserEntity> userOptional = userRepository.findByUserName(username);
        if (userOptional.isEmpty()) {
            log.warn("[LOGIN_DEBUG] User not found in database - username: {}", username);
            throw new RuntimeException("User not found.");
        }
        
        UserEntity userEntity = userOptional.get();
        log.debug("[LOGIN_DEBUG] User found - userId: {}, userName: {}", userEntity.getUserId(), username);
        log.debug("[LOGIN_DEBUG] Password hash exists: {}", userEntity.getPassword() != null && !userEntity.getPassword().isEmpty());

        UserDetails userDetails = User.builder()
                .username(userEntity.getUserName())
                .password(userEntity.getPassword())
                .build();
        
        log.debug("[LOGIN_DEBUG] UserDetails built successfully");
        return userDetails;
    }
}
