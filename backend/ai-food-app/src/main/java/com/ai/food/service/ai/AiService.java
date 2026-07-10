package com.ai.food.service.ai;

import com.ai.food.common.ai.ChatResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
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
    private String recommendationPrompt;

    public AiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostConstruct
    public void loadPrompts() {
        questionGenerationPrompt = loadPrompt("prompts/question-generation.txt");
        recommendationPrompt = loadPrompt("prompts/recommendation.txt");
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

    /**
     * 调用 LLM 生成文本，并把 token 用量 + 模型名一并返回（用于记账到 qa_record）。
     *
     * @param systemPrompt 系统 prompt
     * @param userMessage  用户消息
     * @return ChatResult，包含 text + 三个 token 计数 + model；失败兜底 text 也带在 result 里
     */
    public ChatResult chat(String systemPrompt, String userMessage) {
        try {
            Message system = new SystemMessage(systemPrompt);
            Message user = new UserMessage(userMessage);
            Prompt prompt = new Prompt(List.of(system, user));

            ChatResponse response = chatModel.call(prompt);
            String text = response.getResult().getOutput().getText();

            // ponytail: 捕获 token 用量。usage/metadata 缺失或异常只 log,不影响文本返回。
            Long promptTokens = null;
            Long completionTokens = null;
            Long totalTokens = null;
            String model = null;
            try {
                ChatResponseMetadata metadata = response.getMetadata();
                if (metadata != null) {
                    Usage usage = metadata.getUsage();
                    if (usage != null) {
                        Integer pt = usage.getPromptTokens();
                        Integer ct = usage.getCompletionTokens();
                        Integer tt = usage.getTotalTokens();
                        promptTokens = pt != null ? pt.longValue() : null;
                        completionTokens = ct != null ? ct.longValue() : null;
                        totalTokens = tt != null ? tt.longValue() : null;
                    }
                    model = metadata.getModel();
                }
            } catch (Exception usageEx) {
                log.warn("Failed to read token usage from ChatResponse: {}", usageEx.getMessage());
            }

            return new ChatResult(text, promptTokens, completionTokens, totalTokens, model);
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            return ChatResult.of("抱歉，AI服务暂时不可用，请稍后重试");
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

        return chat("你是一个友好的美食推荐助手。", prompt).getText();
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

        return chat("你是一个专业的美食推荐助手。", prompt).getText();
    }

}
