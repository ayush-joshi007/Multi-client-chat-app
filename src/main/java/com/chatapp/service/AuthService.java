package com.chatapp.service;


import com.chatapp.Mapper.impl.RegisterRequestMapper;
import com.chatapp.Security.JwtService;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.LoginResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.UserEntity;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class AuthService {

    private UserRepository userRepository;
    private RegisterRequestMapper registerRequestMapper;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;

    public boolean register(RegisterRequest registerRequest) {

        UserEntity userEntity = registerRequestMapper.mapFrom(registerRequest);

        String email = registerRequest.getEmail();
        Optional<UserEntity> foundUserEmail =
                userRepository.findByEmail(email);

        String userName = registerRequest.getUserName();
        Optional<UserEntity> foundUserName =
                userRepository.findByUserName(userName);

        if(foundUserEmail.isPresent() || foundUserName.isPresent()){
            return false;
        }

        String password = registerRequest.getPassword();
        String hashedPassword =
                passwordEncoder.encode(password);

        userEntity.setPassword(hashedPassword);

        userRepository.save(userEntity);

        return true;
    }
    public LoginResponse login(LoginRequest loginRequest){

        String userName = loginRequest.getUserName();
        String password = loginRequest.getPassword();
        
        log.info("[LOGIN_DEBUG] Attempting login for username: {}", userName);

        try {
            log.debug("[LOGIN_DEBUG] Calling authenticationManager.authenticate()");
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userName,
                            password
                    )
            );
            log.info("[LOGIN_DEBUG] authenticationManager.authenticate() succeeded for username: {}", userName);
        } catch (Exception e) {
            log.error("[LOGIN_DEBUG] authenticationManager.authenticate() failed - Exception class: {}, Message: {}", 
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }

        log.debug("[LOGIN_DEBUG] Generating JWT token for username: {}", userName);
        String token = jwtService.generateToken(userName);
        log.debug("[LOGIN_DEBUG] JWT token generated successfully");

        UserEntity userEntity = userRepository.findByUserName(userName)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        log.info("[LOGIN_DEBUG] Login successful for userId: {}, userName: {}", userEntity.getUserId(), userName);
        return new LoginResponse(
                userEntity.getUserId(),
                userEntity.getUserName(),
                token
        );
    }


}
