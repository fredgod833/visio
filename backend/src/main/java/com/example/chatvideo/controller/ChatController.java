package com.example.chatvideo.controller;

import com.example.chatvideo.dto.ChatMessageDTO;
import com.example.chatvideo.dto.VideoSignalDTO;
import com.example.chatvideo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gérer les messages de chat
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO message, Principal principal) {
        message.setSender(principal.getName());
        message.setRoomId(message.getRoomId());
        return chatService.saveAndBroadcastMessage(message);
    }

    /**
     * Notification de join
     */
    @MessageMapping("/chat.join")
    @SendTo("/topic/notifications")
    public ChatMessageDTO joinChat(@Payload ChatMessageDTO message,
                                    SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", message.getSender());
        message.setRoomId(message.getRoomId());
        message.setType("JOIN");
        return message;
    }

    /**
     * Signalisation WebRTC - Offer
     */
    @MessageMapping("/video.offer")
    public void handleVideoOffer(@Payload VideoSignalDTO signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(
            signal.getTo(), 
            "/queue/video.offer", 
            signal
        );
    }

    /**
     * Signalisation WebRTC - Answer
     */
    @MessageMapping("/video.answer")
    public void handleVideoAnswer(@Payload VideoSignalDTO signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(
            signal.getTo(), 
            "/queue/video.answer", 
            signal
        );
    }

    /**
     * Signalisation WebRTC - ICE Candidate
     */
    @MessageMapping("/video.ice")
    public void handleIceCandidate(@Payload VideoSignalDTO signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(
            signal.getTo(), 
            "/queue/video.ice", 
            signal
        );
    }

    /**
     * Demande d'appel vidéo
     */
    @MessageMapping("/video.call")
    public void initiateVideoCall(@Payload VideoSignalDTO signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(
            signal.getTo(), 
            "/queue/video.call", 
            signal
        );
    }
}