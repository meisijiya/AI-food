package com.ai.food.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientMessage {
    
    private String action; // start/answer/complete
    private String sessionId;
    private String content;
}