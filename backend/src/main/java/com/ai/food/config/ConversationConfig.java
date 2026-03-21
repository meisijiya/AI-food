package com.ai.food.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class ConversationConfig {

    private Conversation conversation = new Conversation();
    private RandomMode randomMode = new RandomMode();
    private Params params = new Params();

    @Data
    public static class Conversation {
        private int minQuestions = 5;
        private int maxQuestions = 10;
        private int maxParamsRetry = 2;
        private int maxInterrupt = 10;
    }

    @Data
    public static class RandomMode {
        private double similarityThreshold = 0.7;
    }

    @Data
    public static class Params {
        private List<String> required = List.of("time", "location", "weather", "mood", "companion", "budget", "taste");
        private List<String> optional = List.of("restriction", "preference", "health");
    }
}
