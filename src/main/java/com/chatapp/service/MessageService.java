package com.chatapp.service;

import com.chatapp.Mapper.impl.MessageMapper;
import com.chatapp.dto.MessageDto;
import com.chatapp.entity.MessageEntity;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private final MessageMapper messageMapper;
    private final ModelMapper modelMapper;

    public MessageDto sendMessage(MessageDto messageDto){

        MessageEntity messageEntity = messageMapper.mapFrom(messageDto);

        if(messageDto.getSenderId() != null){
            userRepository.findById(messageDto.getSenderId())
                    .ifPresent(messageEntity::setSender);
        }
        MessageEntity savedMessageEntity =  messageRepository.save(messageEntity);
        MessageDto responseDto = messageMapper.mapTo(savedMessageEntity);

        if(savedMessageEntity.getSender() != null){
            responseDto.setUserName(savedMessageEntity.getSender().getUserName());
            responseDto.setSenderId(savedMessageEntity.getSender().getUserId());

        }

        return responseDto;
    }

    public List<MessageDto> getHistory() {
        Iterable<MessageEntity> result= messageRepository.findAll(Sort.by("createdAt"));
        List<MessageDto> li = new ArrayList<>();
        for(MessageEntity m: result){
            MessageDto msgDto = messageMapper.mapTo(m);
            if(m.getSender() != null){
                msgDto.setUserName(m.getSender().getUserName());
                msgDto.setSenderId(m.getSender().getUserId());
            }
            li.add(msgDto);
        }
        return li;
    }


}
