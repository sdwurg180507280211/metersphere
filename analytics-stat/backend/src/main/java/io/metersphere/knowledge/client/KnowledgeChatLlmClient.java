package io.metersphere.knowledge.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
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
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
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

    public String streamAnswer(String question, List<KnowledgeChatSource> sources, List<HistoryTurn> history, Consumer<String> onDelta) {
        return streamAnswer(question, sources, history, null, null, onDelta);
    }

    public String streamAnswer(String question,
                               List<KnowledgeChatSource> sources,
                               List<HistoryTurn> history,
                               String modelOverride,
                               BooleanSupplier shouldStop,
                               Consumer<String> onDelta) {
        if (!available()) {
            return null;
        }

        try {
            MessageCreateParams params = buildKnowledgeParams(question, sources, history, modelOverride);
            StringBuilder answer = new StringBuilder();
            try (StreamResponse<RawMessageStreamEvent> stream = anthropicClient.messages().createStreaming(params)) {
                stream.stream().forEach(event -> {
                    throwIfCancelled(shouldStop);
                    String delta = extractDelta(event);
                    if (StringUtils.isNotBlank(delta)) {
                        throwIfCancelled(shouldStop);
                        answer.append(delta);
                        onDelta.accept(delta);
                    }
                });
            }
            return answer.isEmpty() ? null : answer.toString();
        } catch (CancellationException e) {
            logger.info("LLM 流式调用已取消");
            throw e;
        } catch (Exception e) {
            logger.warn("LLM 流式调用失败，降级为模板回答: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 普通对话流式接口（不使用 RAG）
     */
    public String streamNormalChat(String question, List<HistoryTurn> history, Consumer<String> onDelta) {
        return streamNormalChat(question, history, null, null, onDelta);
    }

    public String streamNormalChat(String question,
                                   List<HistoryTurn> history,
                                   String modelOverride,
                                   BooleanSupplier shouldStop,
                                   Consumer<String> onDelta) {
        if (!available()) {
            return null;
        }

        try {
            MessageCreateParams params = buildNormalChatParams(question, history, modelOverride);
            StringBuilder answer = new StringBuilder();
            try (StreamResponse<RawMessageStreamEvent> stream = anthropicClient.messages().createStreaming(params)) {
                stream.stream().forEach(event -> {
                    throwIfCancelled(shouldStop);
                    String delta = extractDelta(event);
                    if (StringUtils.isNotBlank(delta)) {
                        throwIfCancelled(shouldStop);
                        answer.append(delta);
                        onDelta.accept(delta);
                    }
                });
            }
            return answer.isEmpty() ? null : answer.toString();
        } catch (CancellationException e) {
            logger.info("LLM 普通对话流式调用已取消");
            throw e;
        } catch (Exception e) {
            logger.warn("LLM 普通对话流式调用失败: {}", e.getMessage());
            return null;
        }
    }

    private MessageCreateParams buildKnowledgeParams(String question,
                                                     List<KnowledgeChatSource> sources,
                                                     List<HistoryTurn> history,
                                                     String modelOverride) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(Model.of(resolveModel(modelOverride)))
                .system(buildSystemPrompt())
                .temperature(temperature)
                .maxTokens((long) maxTokens)
                .topP(topP);

        appendHistory(builder, history);
        builder.addUserMessage(buildUserPrompt(question, sources));
        return builder.build();
    }

    private MessageCreateParams buildNormalChatParams(String question, List<HistoryTurn> history, String modelOverride) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(Model.of(resolveModel(modelOverride)))
                .system("你是一个有帮助的AI助手。请直接回答用户的问题。")
                .temperature(temperature)
                .maxTokens((long) maxTokens)
                .topP(topP);

        appendHistory(builder, history);
        builder.addUserMessage(question);
        return builder.build();
    }

    private void appendHistory(MessageCreateParams.Builder builder, List<HistoryTurn> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
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

    private String resolveModel(String modelOverride) {
        return StringUtils.defaultIfBlank(modelOverride, model);
    }

    private String buildSystemPrompt() {
        if (StringUtils.isNotBlank(promptRules)) {
            return promptRules.trim();
        }
        return "你是企业知识库助手。你必须仅基于提供的引用来源回答。"
                + "输出要求：先给结论，再给依据；每个关键结论都在句末标注(来源#编号: 文件名)；"
                + "若证据不足，明确回答“暂无相关信息”并说明缺失的上下文。";
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

    private void throwIfCancelled(BooleanSupplier shouldStop) {
        if (shouldStop != null && shouldStop.getAsBoolean()) {
            throw new CancellationException("request cancelled");
        }
    }

    public List<Map<String, String>> listModels() {
        if (!available()) {
            return List.of();
        }
        try {
            return anthropicClient.models().list().data().stream()
                    .map(m -> {
                        Map<String, String> modelMap = new java.util.HashMap<>();
                        modelMap.put("id", m.id());
                        String name = m.id();
                        try {
                            String displayName = m.displayName();
                            if (displayName != null && !displayName.isEmpty()) {
                                name = displayName;
                            }
                        } catch (Exception ignored) {
                        }
                        modelMap.put("name", name);
                        return modelMap;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.warn("获取模型列表失败，返回默认列表: {}", e.getMessage());
            Map<String, String> defaultModel = new java.util.HashMap<>();
            defaultModel.put("id", model);
            defaultModel.put("name", model);
            return List.of(defaultModel);
        }
    }

    public record HistoryTurn(String userQuestion, String assistantAnswer) {
    }
}
