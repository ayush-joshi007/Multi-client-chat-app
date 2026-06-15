package com.chatapp.Mapper.impl;

import com.chatapp.Mapper.Mapper;
import com.chatapp.dto.LoginRequest;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;


@AllArgsConstructor
@Component
public class LoginRequestMapper implements Mapper<UserEntity, LoginRequest> {

    private ModelMapper modelMapper;

    @Override
    public UserEntity mapFrom(LoginRequest loginRequest) {
        return modelMapper.map(loginRequest, UserEntity.class);
    }

    @Override
    public LoginRequest mapTo(UserEntity userEntity) {
        return modelMapper.map(userEntity, LoginRequest.class);
    }
}
