package com.chatapp.service;

import com.chatapp.entity.UserEntity;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.UserDetailsManagerConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserEntity> userOptional = userRepository.findByUserName(username);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found.");
        }
        
        UserEntity userEntity = userOptional.get();

        return User.builder()
                .username(userEntity.getUserName())
                .password(userEntity.getPassword())
                .build();
    }
}
