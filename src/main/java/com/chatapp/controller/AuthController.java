package com.chatapp.controller;

import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.LoginResponse;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.UserEntity;
import com.chatapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest){
        boolean success= authService.register(registerRequest);
        if(success){
            return ResponseEntity.status(200).body("Registration Successful!");
        }
        else{
            return ResponseEntity.status(409).body("Account Already Exists!");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        UserEntity user = authService.login(loginRequest);

        if (user != null) {
            return ResponseEntity.ok(
                    new LoginResponse(
                            user.getUserId(),
                            user.getUserName()
                    )
            );
        }

        return ResponseEntity.status(401)
                .body("Incorrect Credentials!");
    }
}
