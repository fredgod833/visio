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
public class VideoSignalDTO {
    private String type; // OFFER, ANSWER, ICE_CANDIDATE, CALL_REQUEST
    private String from;
    private String to;
    private String roomId = "Assistance";
    private Object signal; // SDP ou ICE candidate
}