package com.chatapp.Mapper.impl;

import com.chatapp.Mapper.Mapper;
import com.chatapp.dto.MessageDto;
import com.chatapp.entity.MessageEntity;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MessageMapper implements Mapper<MessageEntity, MessageDto> {

    private ModelMapper modelMapper;

    @Override
    public MessageEntity mapFrom(MessageDto messageDto) {


        MessageEntity messageEntity = new MessageEntity();

        messageEntity.setContent(messageDto.getContent());
        messageEntity.setStatus(messageDto.getStatus());

        return messageEntity;
    }

    @Override
    public MessageDto mapTo(MessageEntity messageEntity) {

        MessageDto dto = modelMapper.map(messageEntity, MessageDto.class);

        if (messageEntity.getSender() != null) {
            dto.setUserName(messageEntity.getSender().getUserName());
            dto.setSenderId(messageEntity.getSender().getUserId());
        }

        if (messageEntity.getReceiver() != null) {
            dto.setReceiverId(messageEntity.getReceiver().getUserId());
        }

        return dto;
    }
}