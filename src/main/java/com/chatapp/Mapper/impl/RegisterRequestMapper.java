package com.chatapp.Mapper.impl;

import com.chatapp.Mapper.Mapper;
import com.chatapp.dto.RegisterRequest;
import com.chatapp.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;


@AllArgsConstructor
@Component
public class RegisterRequestMapper implements Mapper<UserEntity, RegisterRequest> {

    private ModelMapper modelMapper;

    @Override
    public UserEntity mapFrom(RegisterRequest registerRequest) {
        return modelMapper.map(registerRequest, UserEntity.class);
    }

    @Override
    public RegisterRequest mapTo(UserEntity userEntity) {
        return modelMapper.map(userEntity, RegisterRequest.class);
    }
}
