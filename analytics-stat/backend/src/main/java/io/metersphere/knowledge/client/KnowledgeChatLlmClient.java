package io.metersphere.knowledge.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.RawContentBlockDelta;
import com.anthropic.models.messages.RawContentBlockDeltaEvent;
import com.anthropic.models.messages.RawMessageStreamEvent;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 知识问答大模型客户端（Claude Java SDK）
 */
@Component
public class KnowledgeChatLlmClient {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatLlmClient.class);

    @Value("${chat.api.enabled:false}")
    private boolean enabled;

    @Value("${chat.api.url:}")
    private String apiUrl;

    @Value("${chat.api.key:}")
    private String apiKey;

    @Value("${chat.api.model:}")
    private String model;

    @Value("${chat.api.temperature:0.8}")
    private double temperature;

    @Value("${chat.api.max-tokens:600}")
    private int maxTokens;

    @Value("${chat.api.top-p:0.9}")
    private double topP;

    @Value("${chat.api.prompt-rules:}")
    private String promptRules;

    private final AnthropicClient anthropicClient;

    public KnowledgeChatLlmClient(AnthropicClient anthropicClient) {
        this.anthropicClient = anthropicClient;
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            logger.info("KnowledgeChatLlmClient 未启用，知识问答将使用模板回答");
            return;
        }
        if (StringUtils.isBlank(apiUrl) || StringUtils.isBlank(apiKey) || StringUtils.isBlank(model)) {
            logger.warn("KnowledgeChatLlmClient 配置不完整，将自动降级为模板回答");
            return;
        }
        logger.info("KnowledgeChatLlmClient 已启用 - model: {}, url: {}", model, apiUrl);
    }

    public boolean available() {
        return enabled
                && StringUtils.isNotBlank(apiUrl)
                && StringUtils.isNotBlank(apiKey)
                && StringUtils.isNotBlank(model);
    }

    public String generateAnswer(String question, List<KnowledgeChatSource> sources, List<HistoryTurn> history) {
        if (!available()) {
            return null;
        }

        try {
            MessageCreateParams params = buildParams(question, sources, history);

            Message message = anthropicClient.messages().create(params);
            return parseAnswer(message);
        } catch (Exception e) {
            logger.warn("LLM 调用失败，降级为模板回答: {}", e.getMessage());
            return null;
        }
    }

    public String streamAnswer(String question, List<KnowledgeChatSource> sources, List<HistoryTurn> history, Consumer<String> onDelta) {
        if (!available()) {
            return null;
        }

        try {
            MessageCreateParams params = buildParams(question, sources, history);
            StringBuilder answer = new StringBuilder();
            try (StreamResponse<RawMessageStreamEvent> stream = anthropicClient.messages().createStreaming(params)) {
                stream.stream().forEach(event -> {
                    String delta = extractDelta(event);
                    if (StringUtils.isNotBlank(delta)) {
                        answer.append(delta);
                        onDelta.accept(delta);
                    }
                });
            }
            return answer.isEmpty() ? null : answer.toString();
        } catch (Exception e) {
            logger.warn("LLM 流式调用失败，降级为模板回答: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 普通对话流式接口（不使用 RAG）
     */
    public String streamNormalChat(String question, List<HistoryTurn> history, Consumer<String> onDelta) {
        if (!available()) {
            return null;
        }

        try {
            MessageCreateParams params = buildNormalChatParams(question, history);
            StringBuilder answer = new StringBuilder();
            try (StreamResponse<RawMessageStreamEvent> stream = anthropicClient.messages().createStreaming(params)) {
                stream.stream().forEach(event -> {
                    String delta = extractDelta(event);
                    if (StringUtils.isNotBlank(delta)) {
                        answer.append(delta);
                        onDelta.accept(delta);
                    }
                });
            }
            return answer.isEmpty() ? null : answer.toString();
        } catch (Exception e) {
            logger.warn("LLM 普通对话流式调用失败: {}", e.getMessage());
            return null;
        }
    }

    private MessageCreateParams buildParams(String question, List<KnowledgeChatSource> sources, List<HistoryTurn> history) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(Model.of(model))
                .system(buildSystemPrompt())
                .temperature(temperature)
                .maxTokens((long) maxTokens)
                .topP(topP);

        if (history != null && !history.isEmpty()) {
            for (HistoryTurn turn : history) {
                if (turn == null) {
                    continue;
                }
                if (StringUtils.isNotBlank(turn.userQuestion())) {
                    builder.addUserMessage(turn.userQuestion());
                }
                if (StringUtils.isNotBlank(turn.assistantAnswer())) {
                    builder.addAssistantMessage(turn.assistantAnswer());
                }
            }
        }

        builder.addUserMessage(buildUserPrompt(question, sources));
        return builder.build();
    }

    /**
     * 构建普通对话参数（不使用知识库上下文）
     */
    private MessageCreateParams buildNormalChatParams(String question, List<HistoryTurn> history) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(Model.of(model))
                .system("你是一个有帮助的AI助手。请直接回答用户的问题。")
                .temperature(temperature)
                .maxTokens((long) maxTokens)
                .topP(topP);

        if (history != null && !history.isEmpty()) {
            for (HistoryTurn turn : history) {
                if (turn == null) {
                    continue;
                }
                if (StringUtils.isNotBlank(turn.userQuestion())) {
                    builder.addUserMessage(turn.userQuestion());
                }
                if (StringUtils.isNotBlank(turn.assistantAnswer())) {
                    builder.addAssistantMessage(turn.assistantAnswer());
                }
            }
        }

        builder.addUserMessage(question);
        return builder.build();
    }

    private String buildSystemPrompt() {
        if (StringUtils.isNotBlank(promptRules)) {
            return promptRules.trim();
        }
        return "你是企业知识库助手。你必须仅基于提供的引用来源回答。" +
                "输出要求：先给结论，再给依据；每个关键结论都在句末标注(来源#编号: 文件名)；" +
                "若证据不足，明确回答“暂无相关信息”并说明缺失的上下文。";
    }

    private String buildUserPrompt(String question, List<KnowledgeChatSource> sources) {
        StringBuilder sb = new StringBuilder();
        sb.append("问题:\n").append(question).append("\n\n");
        sb.append("引用来源:\n");
        for (int i = 0; i < sources.size(); i++) {
            KnowledgeChatSource source = sources.get(i);
            sb.append(i + 1)
                    .append(". [")
                    .append(source.getFileName())
                    .append("#")
                    .append(source.getChunkId())
                    .append("] ")
                    .append(source.getSnippet())
                    .append("\n");
        }
        sb.append("\n请严格只基于以上来源回答，且按“(来源#编号: 文件名)”标注引用。\n");
        return sb.toString();
    }

    private String extractDelta(RawMessageStreamEvent event) {
        if (event == null || !event.contentBlockDelta().isPresent()) {
            return null;
        }
        RawContentBlockDeltaEvent deltaEvent = event.contentBlockDelta().get();
        RawContentBlockDelta delta = deltaEvent.delta();
        if (delta == null || !delta.text().isPresent()) {
            return null;
        }
        return delta.text().get().text();
    }

    private String parseAnswer(Message message) {
        if (message == null || message.content() == null || message.content().isEmpty()) {
            return null;
        }

        StringBuilder answer = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block != null && block.text().isPresent()) {
                String text = block.text().get().text();
                if (StringUtils.isNotBlank(text)) {
                    answer.append(text);
                }
            }
        }

        return answer.isEmpty() ? null : answer.toString();
    }

    public record HistoryTurn(String userQuestion, String assistantAnswer) {
    }
}
