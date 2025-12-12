package com.example.chatvideo.service;

import com.example.chatvideo.dto.ChatMessageDTO;
import com.example.chatvideo.exceptions.InvalidUserException;
import com.example.chatvideo.model.ChatMessage;
import com.example.chatvideo.model.ChatRoom;
import com.example.chatvideo.model.MessageType;
import com.example.chatvideo.model.UserEntity;
import com.example.chatvideo.repository.ChatMessageRepository;
import com.example.chatvideo.repository.ChatRoomRepository;
import com.example.chatvideo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageDTO saveAndBroadcastMessage(ChatMessageDTO messageDTO) {

        try {
            UserEntity sender = userRepository.findByUsername(messageDTO.getSender()).orElseThrow(() -> new InvalidUserException("Utilisateur inconnu !"));

            ChatRoom room = roomRepository.findByRoomId(messageDTO.getRoomId())
                    .orElseGet(() -> createRoom(messageDTO.getRoomId()));

            ChatMessage message = ChatMessage.builder()
                    .content(messageDTO.getContent())
                    .sender(sender)
                    .room(room)
                    .type(MessageType.valueOf(messageDTO.getType()))
                    .timestamp(LocalDateTime.now())
                    .isRead(false)
                    .build();

            ChatMessage savedMessage = messageRepository.save(message);
            return convertToDTO(savedMessage);

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du message", e);
            messageDTO.setTimestamp(LocalDateTime.now());
            return messageDTO;
        }

    }

    public List<ChatMessageDTO> getMessageHistory(String roomId, int limit) {
        ChatRoom room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Room non trouvée: " + roomId));

        return messageRepository.findByRoomOrderByTimestampDesc(room)
                .stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ChatRoom createRoom(String roomId) {
        ChatRoom room = ChatRoom.builder()
                .roomId(roomId)
                .name(roomId)
                .createdAt(LocalDateTime.now())
                .build();
        return roomRepository.save(room);
    }

    private ChatMessageDTO convertToDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .sender(message.getSender().getUsername())
                .roomId(message.getRoom().getRoomId())
                .type(message.getType().name())
                .timestamp(message.getTimestamp())
                .build();
    }
}
