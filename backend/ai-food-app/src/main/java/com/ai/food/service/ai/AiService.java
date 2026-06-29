package com.ai.food.service.ai;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class AiService {

    private final ChatModel chatModel;

    private String questionGenerationPrompt;
    private String answerValidationPrompt;
    private String recommendationPrompt;
    private String similarityPrompt;

    public AiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostConstruct
    public void loadPrompts() {
        questionGenerationPrompt = loadPrompt("prompts/question-generation.txt");
        answerValidationPrompt = loadPrompt("prompts/answer-validation.txt");
        recommendationPrompt = loadPrompt("prompts/recommendation.txt");
        similarityPrompt = loadPrompt("prompts/similarity.txt");
        log.info("AI prompt templates loaded successfully");
    }

    private String loadPrompt(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to load prompt template: {}", path, e);
            return "";
        }
    }

    public String chat(String systemPrompt, String userMessage) {
        try {
            Message system = new SystemMessage(systemPrompt);
            Message user = new UserMessage(userMessage);
            Prompt prompt = new Prompt(List.of(system, user));

            var response = chatModel.call(prompt);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            return "抱歉，AI服务暂时不可用，请稍后重试";
        }
    }

    /**
     * 基于已收集的对话上下文，为指定参数类型生成自然的提问
     *
     * @param param   参数类型（restriction/preference/health）
     * @param context 已收集的参数信息摘要
     * @return AI生成的提问文本
     */
    public String generateQuestion(String param, String context) {
        String prompt = questionGenerationPrompt
                .replace("{param}", param != null ? param : "")
                .replace("{context}", context != null ? context : "");

        return chat("你是一个友好的美食推荐助手。", prompt);
    }

    /**
     * 校验用户回答是否有效
     *
     * @param param    参数类型
     * @param question 问题内容
     * @param answer   用户回答
     * @return 是否有效
     */
    public boolean validateAnswer(String param, String question, String answer) {
        String prompt = answerValidationPrompt
                .replace("{param}", param != null ? param : "")
                .replace("{question}", question != null ? question : "")
                .replace("{answer}", answer != null ? answer : "");

        String result = chat("你是一个回答验证助手。", prompt);
        return "true".equalsIgnoreCase(result.trim());
    }

    /**
     * 根据收集的用户信息生成美食推荐
     *
     * @param collectedParams 收集到的参数信息
     * @return JSON格式的推荐结果
     */
    public String generateRecommendation(String collectedParams) {
        String prompt = recommendationPrompt
                .replace("{params}", collectedParams != null ? collectedParams : "");

        return chat("你是一个专业的美食推荐助手。", prompt);
    }

    /**
     * 计算两种食物的相似度
     *
     * @param food1 食物A
     * @param food2 食物B
     * @return 相似度评分（0-1）
     */
    public double calculateSimilarity(String food1, String food2) {
        String prompt = similarityPrompt
                .replace("{food1}", food1 != null ? food1 : "")
                .replace("{food2}", food2 != null ? food2 : "");

        try {
            String result = chat("你是一个食物相似度评估助手。", prompt);
            return Double.parseDouble(result.trim());
        } catch (NumberFormatException e) {
            log.error("Error parsing similarity score", e);
            return 0.5;
        }
    }
}
