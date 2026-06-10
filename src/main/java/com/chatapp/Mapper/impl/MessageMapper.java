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
        return modelMapper.map(messageDto, MessageEntity.class);
    }

    @Override
    public MessageDto mapTo(MessageEntity messageEntity) {
        return modelMapper.map(messageEntity, MessageDto.class);
    }
}
