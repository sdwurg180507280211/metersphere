package io.metersphere.knowledge.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 知识问答大模型客户端（OpenAI 兼容接口）
 */
@Component
public class KnowledgeChatLlmClient {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatLlmClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    @Value("${chat.api.timeout-seconds:30}")
    private int timeoutSeconds;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout())
                .build();
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
            return streamChat(modelOverride, buildSystemPrompt(), history, buildUserPrompt(question, sources), shouldStop, onDelta);
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
            return streamChat(modelOverride, "你是一个有帮助的AI助手。请直接回答用户的问题。", history, question, shouldStop, onDelta);
        } catch (CancellationException e) {
            logger.info("LLM 普通对话流式调用已取消");
            throw e;
        } catch (Exception e) {
            logger.warn("LLM 普通对话流式调用失败: {}", e.getMessage());
            return null;
        }
    }

    private String streamChat(String modelOverride,
                              String systemPrompt,
                              List<HistoryTurn> history,
                              String userContent,
                              BooleanSupplier shouldStop,
                              Consumer<String> onDelta) throws Exception {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", resolveModel(modelOverride));
        body.put("stream", true);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("top_p", topP);
        ArrayNode messages = body.putArray("messages");
        addMessage(messages, "system", systemPrompt);
        appendHistory(messages, history);
        addMessage(messages, "user", userContent);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint("/chat/completions")))
                .timeout(timeout())
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            try (InputStream in = response.body()) {
                throw new IllegalStateException("HTTP " + response.statusCode() + ": "
                        + new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                throwIfCancelled(shouldStop);
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                String delta = extractDelta(data);
                if (StringUtils.isNotEmpty(delta)) {
                    throwIfCancelled(shouldStop);
                    answer.append(delta);
                    onDelta.accept(delta);
                }
            }
        }
        return answer.isEmpty() ? null : answer.toString();
    }

    private void addMessage(ArrayNode messages, String role, String content) {
        ObjectNode message = messages.addObject();
        message.put("role", role);
        message.put("content", content);
    }

    private void appendHistory(ArrayNode messages, List<HistoryTurn> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        for (HistoryTurn turn : history) {
            if (turn == null) {
                continue;
            }
            if (StringUtils.isNotBlank(turn.userQuestion())) {
                addMessage(messages, "user", turn.userQuestion());
            }
            if (StringUtils.isNotBlank(turn.assistantAnswer())) {
                addMessage(messages, "assistant", turn.assistantAnswer());
            }
        }
    }

    private String endpoint(String path) {
        String base = StringUtils.removeEnd(apiUrl.trim(), "/");
        base = StringUtils.removeEnd(base, "/chat/completions");
        return base + path;
    }

    private Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 30);
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

    private String extractDelta(String data) {
        try {
            JsonNode content = OBJECT_MAPPER.readTree(data).path("choices").path(0).path("delta").path("content");
            return content.isTextual() ? content.asText() : null;
        } catch (Exception e) {
            return null;
        }
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint("/models")))
                    .timeout(timeout())
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            List<Map<String, String>> models = new ArrayList<>();
            for (JsonNode node : OBJECT_MAPPER.readTree(response.body()).path("data")) {
                String id = node.path("id").asText();
                if (StringUtils.isBlank(id)) {
                    continue;
                }
                Map<String, String> modelMap = new HashMap<>();
                modelMap.put("id", id);
                modelMap.put("name", id);
                models.add(modelMap);
            }
            if (models.isEmpty()) {
                throw new IllegalStateException("empty model list");
            }
            return models;
        } catch (Exception e) {
            logger.warn("获取模型列表失败，返回默认列表: {}", e.getMessage());
            Map<String, String> defaultModel = new HashMap<>();
            defaultModel.put("id", model);
            defaultModel.put("name", model);
            return List.of(defaultModel);
        }
    }

    public record HistoryTurn(String userQuestion, String assistantAnswer) {
    }
}
