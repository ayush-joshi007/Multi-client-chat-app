package com.chatapp.service;

import com.chatapp.Mapper.impl.MessageMapper;
import com.chatapp.dto.MessageDto;
import com.chatapp.entity.MessageEntity;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private final MessageMapper messageMapper;

    public MessageDto sendMessage(MessageDto messageDto){

        System.out.println("NOW = " + LocalDateTime.now());


        MessageEntity messageEntity = messageMapper.mapFrom(messageDto);

        if(messageDto.getSenderId() != null){
            userRepository.findById(messageDto.getSenderId())
                    .ifPresent(messageEntity::setSender);
        }

        if(messageDto.getReceiverId() != null){
            userRepository.findById(messageDto.getReceiverId())
                    .ifPresent(messageEntity::setReceiver);
        }
        MessageEntity savedMessageEntity =  messageRepository.save(messageEntity);
        MessageDto responseDto = messageMapper.mapTo(savedMessageEntity);

        if(savedMessageEntity.getSender() != null){
            responseDto.setUserName(savedMessageEntity.getSender().getUserName());
            responseDto.setSenderId(savedMessageEntity.getSender().getUserId());

        }
        if(messageDto.getReceiverId() != null){
            userRepository.findById(messageDto.getReceiverId())
                    .ifPresent(messageEntity::setReceiver);
        }

        return responseDto;
    }

    public List<MessageDto> getHistory() {
        Iterable<MessageEntity> result= messageRepository.findByReceiverIsNullOrderByCreatedAt();
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

    public List<MessageDto> getPrivateHistory(Long senderId, Long receiverId){

        List<MessageEntity> result = messageRepository.findPrivateConversation(senderId, receiverId);

        List<MessageDto> messages = new ArrayList<>();

        for(MessageEntity m : result){

            MessageDto msgDto = messageMapper.mapTo(m);

            if(m.getSender() != null){
                msgDto.setUserName(m.getSender().getUserName());
                msgDto.setSenderId(m.getSender().getUserId());
            }

            if(m.getReceiver() != null){
                msgDto.setReceiverId(m.getReceiver().getUserId());
            }

            messages.add(msgDto);
        }

        return messages;
    }


}
