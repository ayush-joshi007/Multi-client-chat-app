package com.chatapp.service;


import com.chatapp.Mapper.impl.RegisterRequestMapper;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.UserEntity;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {

    private UserRepository userRepository;
    private RegisterRequestMapper registerRequestMapper;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public String register(RegisterRequest registerRequest){
        UserEntity userEntity = registerRequestMapper.mapFrom(registerRequest);

        String email = registerRequest.getEmail();
        Optional<UserEntity> foundUserEmail = userRepository.findByEmail(email);
        String userName = registerRequest.getUserName();
        Optional<UserEntity> foundUserName = userRepository.findByUserName(userName);
        if(foundUserEmail.isPresent() || foundUserName.isPresent()){
            return "Account Already Exists!";
        }
        else{
            String password = registerRequest.getPassword();
            String hashedPassword = bCryptPasswordEncoder.encode(password);
            userEntity.setPassword(hashedPassword);
            userRepository.save(userEntity);

            return "Registration Successful!";
        }

    }
    public String  login(LoginRequest loginRequest){

        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        Optional<UserEntity> foundUserEntity = userRepository.findByEmail(email);
        if(foundUserEntity.isPresent()){
            UserEntity userEntity = foundUserEntity.get();
            String dbPassword = userEntity.getPassword();

            if(bCryptPasswordEncoder.matches(password, dbPassword)){
                return "Login Successful!";
            }
            else{
                return "Incorrect Credentials!";
            }
        }
        else{
            return "Incorrect Credentials!";
        }


    }


}
