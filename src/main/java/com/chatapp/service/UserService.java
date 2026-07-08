package com.chatapp.service;

import com.chatapp.Mapper.impl.UserMapper;
import com.chatapp.tracker.OnlineUserTracker;
import com.chatapp.dto.UserDto;
import com.chatapp.entity.UserEntity;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final OnlineUserTracker onlineUserTracker;

    public List<UserDto> getAllUsers(){
        Iterable<UserEntity> userEntities= userRepository.findAll();

        List<UserDto> users= new ArrayList<>();
        for(UserEntity user: userEntities){

            UserDto userDto = userMapper.mapTo(user);

            userDto.setOnline(onlineUserTracker.getOnlineUsers().contains(userDto.getUserId()));

            users.add(userDto);
        }
        return users;
    }

    public Long getUserIdByUserName(String userName){
        return userRepository.findByUserName(userName)
                .map(UserEntity::getUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));
    }
}
