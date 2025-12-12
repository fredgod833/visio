package com.example.chatvideo.repository;

import com.example.chatvideo.model.ChatMessage;
import com.example.chatvideo.model.ChatRoom;
import com.example.chatvideo.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomOrderByTimestampDesc(ChatRoom room);
    List<ChatMessage> findBySender(UserEntity sender);
    List<ChatMessage> findByIsReadFalse();
    
    @Query("SELECT m FROM ChatMessage m WHERE m.room = :room AND m.timestamp >= :since")
    List<ChatMessage> findRecentMessages(ChatRoom room, LocalDateTime since);
    
    void deleteByTimestampBefore(LocalDateTime timestamp);
    long countByRoomAndIsReadFalse(ChatRoom room);
}
