package com.chatapp.repository;

import com.chatapp.entity.MessageEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends CrudRepository<MessageEntity, Long> {

    Iterable<MessageEntity> findByReceiverIsNullOrderByCreatedAt();

    @Query("""
    SELECT m
    FROM MessageEntity m
    WHERE
        (m.sender.userId = :user1Id
         AND m.receiver.userId = :user2Id)
    OR
        (m.sender.userId = :user2Id
         AND m.receiver.userId = :user1Id)
    ORDER BY m.createdAt
""")
    List<MessageEntity> findPrivateConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

}
