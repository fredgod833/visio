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
    private Integer id;
    private String sender;
    private String content;
    private Integer agencyId;
    private Integer customerId;
    private String type; // CHAT, JOIN, LEAVE
    private LocalDateTime timestamp;
}