package com.chatapp.service;


import com.chatapp.Mapper.impl.RegisterRequestMapper;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.LoginResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.UserEntity;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
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

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userName,
                        password
                )
        );

        String token = jwtService.generateToken(userName);

        UserEntity userEntity = userRepository.findByUserName(userName)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );


        return new LoginResponse(
                userEntity.getUserId(),
                userEntity.getUserName(),
                token
        );
    }


}
