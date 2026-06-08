package com.chatapp.repository;

import com.chatapp.entity.MessageEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends CrudRepository<MessageEntity, Long> {
    Iterable<MessageEntity> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
}
