package com.chatapp.controller;

import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.service.AuthService;
import lombok.AllArgsConstructor;
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
    public String register(@RequestBody RegisterRequest registerRequest){
        return authService.register(registerRequest);
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest loginRequest){
        return authService.login(loginRequest);
    }
}
