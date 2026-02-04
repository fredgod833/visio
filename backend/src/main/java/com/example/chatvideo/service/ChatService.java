package com.example.chatvideo.service;

import com.example.chatvideo.dto.ChatMessageDTO;
import com.example.chatvideo.exceptions.InvalidAgencyException;
import com.example.chatvideo.exceptions.InvalidUserException;
import com.example.chatvideo.model.*;
import com.example.chatvideo.repository.AgencyRepository;
import com.example.chatvideo.repository.ChatMessageRepository;
import com.example.chatvideo.repository.CustomerRepository;
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
    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final CustomerRepository customerRepository;


    @Transactional
    public ChatMessageDTO saveAndBroadcastMessage(ChatMessageDTO messageDTO) {

        try {
            UserEntity sender = userRepository.findByEmail(messageDTO.getSender()).orElseThrow(() -> new InvalidUserException("Utilisateur inconnu !"));
            AgencyEntity agency = agencyRepository.findById(messageDTO.getAgencyId()).orElseThrow(() -> new InvalidAgencyException("Agence inconnue !"));
            CustomerEntity customer = customerRepository.findById(messageDTO.getCustomerId()).orElseThrow(() -> new InvalidUserException("Client inconnu !"));;

            MessageEntity message = MessageEntity.builder()
                    .content(messageDTO.getContent())
                    .sender(sender)
                    .agency(agency)
                    .customer(customer)
                    .type(MessageType.valueOf(messageDTO.getType()))
                    .timestamp(LocalDateTime.now())
                    .build();

            MessageEntity savedMessage = messageRepository.save(message);
            return convertToDTO(savedMessage);

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du message", e);
            messageDTO.setTimestamp(LocalDateTime.now());
            return messageDTO;
        }

    }

    public List<ChatMessageDTO> getMessageHistory(int agencyId, int customerId, int limit) throws InvalidAgencyException, InvalidUserException {

        AgencyEntity agency = agencyRepository.findById(agencyId).orElseThrow(() -> new InvalidAgencyException("Agence inconnue !"));
        CustomerEntity customer = customerRepository.findById(customerId).orElseThrow(() -> new InvalidUserException("Client inconnu !"));;
        return messageRepository.findByAgencyAndCustomerOrderByTimestampDesc(agency,customer)
                .stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ChatMessageDTO convertToDTO(MessageEntity message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .sender(message.getSender().getEmail())
                .agencyId(message.getAgency().getId())
                .customerId(message.getCustomer().getId())
                .type(message.getType().name())
                .timestamp(message.getTimestamp())
                .build();
    }
}
