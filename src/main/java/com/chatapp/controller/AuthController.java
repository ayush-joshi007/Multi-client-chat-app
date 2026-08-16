package com.chatapp.controller;

import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.LoginResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.UserEntity;
import com.chatapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.status;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest){
        boolean success= authService.register(registerRequest);
        if(success){
            return status(200).body("Registration Successful!");
        }
        else{
            return status(409).body("Account Already Exists!");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("[LOGIN_DEBUG] AuthController.login() called for username: {}", loginRequest.getUserName());

        try {
            LoginResponse loginResponse= authService.login(loginRequest);
            log.info("[LOGIN_DEBUG] AuthController.login() returning successful response for username: {}", loginRequest.getUserName());
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            log.error("[LOGIN_DEBUG] AuthController.login() caught exception - Exception class: {}, Message: {}", 
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
