package com.example.chatvideo.repository;

import com.example.chatvideo.model.ChatRoom;
import com.example.chatvideo.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByRoomId(String roomId);
    List<ChatRoom> findByType(RoomType type);
    boolean existsByRoomId(String roomId);
}
