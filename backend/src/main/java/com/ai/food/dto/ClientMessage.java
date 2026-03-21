package com.ai.food.dto;

import lombok.Data;

@Data
public class ClientMessage {
    
    private String action; // start/answer/complete
    private String sessionId;
    private String content;
}