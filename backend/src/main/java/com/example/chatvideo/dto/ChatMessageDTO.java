package com.example.chatvideo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private Long id;
    private String sender;
    private String content;
    private String roomId;
    private String type; // CHAT, JOIN, LEAVE
    private LocalDateTime timestamp;
}