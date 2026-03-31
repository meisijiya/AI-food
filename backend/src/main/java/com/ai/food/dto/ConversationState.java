package com.ai.food.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConversationState {
    
    private String sessionId;
    private Boolean aiProcessing = false;
    private List<String> pendingMessages = new ArrayList<>();
    private Integer interruptCount = 0;
    private Integer totalQuestions;
    private Integer currentQuestionCount = 0;
    private List<String> collectedParams = new ArrayList<>();
    private String mode;
    
    private Map<String, String> paramValues = new HashMap<>();
    private Map<String, Integer> paramRetryCount = new HashMap<>();
    private String currentParam;
    private boolean inFreeFormStage = false;
    private boolean cancelled = false;
    
    public ConversationState(String sessionId, Integer totalQuestions, String mode) {
        this.sessionId = sessionId;
        this.totalQuestions = totalQuestions;
        this.mode = mode;
    }
    
    public void addPendingMessage(String message) {
        this.pendingMessages.add(message);
    }
    
    public void clearPendingMessages() {
        this.pendingMessages.clear();
    }
    
    public boolean hasPendingMessages() {
        return !this.pendingMessages.isEmpty();
    }
    
    public boolean canInterrupt() {
        return this.interruptCount < 10;
    }
    
    public void incrementInterruptCount() {
        this.interruptCount++;
    }
    
    public void incrementQuestionCount() {
        this.currentQuestionCount++;
    }
    
    public boolean isCompleted() {
        return this.currentQuestionCount >= this.totalQuestions;
    }
    
    public void saveParamValue(String param, String value) {
        this.paramValues.put(param, value);
        if (!this.collectedParams.contains(param)) {
            this.collectedParams.add(param);
        }
    }
    
    public String getParamValue(String param) {
        return this.paramValues.get(param);
    }
    
    public void incrementParamRetry(String param) {
        this.paramRetryCount.merge(param, 1, Integer::sum);
    }
    
    public int getParamRetryCount(String param) {
        return this.paramRetryCount.getOrDefault(param, 0);
    }
    
    public boolean canRetryParam(String param, int maxRetry) {
        return getParamRetryCount(param) < maxRetry;
    }
    
    public void enterFreeFormStage() {
        this.inFreeFormStage = true;
    }
    
    public boolean isInFreeFormStage() {
        return this.inFreeFormStage;
    }
    
    public void setCurrentParam(String param) {
        this.currentParam = param;
    }
    
    public String getCurrentParam() {
        return this.currentParam;
    }
    
    public boolean isRequiredParam(String param) {
        List<String> required = List.of("time", "location", "weather", "mood", "companion", "budget", "taste");
        return required.contains(param);
    }
    
    public boolean isParamCollected(String param) {
        return this.collectedParams.contains(param);
    }
    
    public int getRequiredParamsCount() {
        List<String> required = List.of("time", "location", "weather", "mood", "companion", "budget", "taste");
        return (int) this.collectedParams.stream()
                .filter(required::contains)
                .count();
    }
}
