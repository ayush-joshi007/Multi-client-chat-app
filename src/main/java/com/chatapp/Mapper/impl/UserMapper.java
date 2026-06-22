package com.chatapp.Mapper.impl;

import com.chatapp.Mapper.Mapper;
import com.chatapp.dto.UserDto;
import com.chatapp.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class UserMapper implements Mapper<UserEntity, UserDto> {

    private final ModelMapper modelMapper;

    @Override
    public UserEntity mapFrom(UserDto userDto) {
        return modelMapper.map(userDto, UserEntity.class);
    }

    @Override
    public UserDto mapTo(UserEntity userEntity) {
        return modelMapper.map(userEntity, UserDto.class);
    }
}
