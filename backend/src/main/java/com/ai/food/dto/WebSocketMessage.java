package com.ai.food.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    private String type; // question/2question/chat/interrupt/recommend/system
    private String param;
    private String content;
    private Progress progress;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private Integer current;
        private Integer total;
        private List<String> collected;
    }
}