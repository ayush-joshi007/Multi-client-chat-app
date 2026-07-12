package com.chatapp.repository;

import com.chatapp.entity.MessageEntity;
import com.chatapp.entity.MessageStatus;
import com.chatapp.projection.UnreadCountProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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


    List<MessageEntity> findBySenderUserIdAndReceiverUserIdAndStatus(
            Long senderId,
            Long receiverId,
            MessageStatus status
    );

    List<MessageEntity> findByReceiverUserIdAndStatus(
            Long receiverId,
            MessageStatus status
    );

    @Query("""
        SELECT
            m.sender.id AS senderId,
            COUNT(m) AS unreadCount
        FROM MessageEntity m
        WHERE
            m.receiver.id = :receiverId
            AND m.status <> com.chatapp.entity.MessageStatus.READ
        GROUP BY m.sender.id
    """)
    List<UnreadCountProjection> findUnreadCountsByReceiverId(@Param("receiverId") Long receiverId);


}
