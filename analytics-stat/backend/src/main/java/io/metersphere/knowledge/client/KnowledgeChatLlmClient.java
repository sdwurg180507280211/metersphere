package io.metersphere.knowledge.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识问答大模型客户端（OpenAI 兼容协议）
 */
@Component
public class KnowledgeChatLlmClient {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatLlmClient.class);

    @Value("${chat.api.enabled:false}")
    private boolean enabled;

    @Value("${chat.api.url:}")
    private String apiUrl;

    @Value("${chat.api.path:/chat/completions}")
    private String apiPath;

    @Value("${chat.api.key:}")
    private String apiKey;

    @Value("${chat.api.model:}")
    private String model;

    @Value("${chat.api.temperature:0.2}")
    private double temperature;

    @Value("${chat.api.max-tokens:600}")
    private int maxTokens;

    @Value("${chat.api.timeout-seconds:30}")
    private int timeoutSeconds;

    private final WebClient knowledgeChatWebClient;
    private final ObjectMapper objectMapper;

    public KnowledgeChatLlmClient(WebClient knowledgeChatWebClient, ObjectMapper objectMapper) {
        this.knowledgeChatWebClient = knowledgeChatWebClient;
        this.objectMapper = objectMapper;
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

    public String generateAnswer(String question, List<KnowledgeChatSource> sources) {
        if (!available()) {
            return null;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", temperature);
            payload.put("max_tokens", maxTokens);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", "你是企业知识库助手。请仅基于给定引用来源回答，并保持简洁。"),
                    Map.of("role", "user", "content", buildUserPrompt(question, sources))
            ));

            String raw = knowledgeChatWebClient.post()
                    .uri(apiPath)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(timeoutSeconds));

            return parseAnswer(raw);
        } catch (Exception e) {
            logger.warn("LLM 调用失败，降级为模板回答: {}", e.getMessage());
            return null;
        }
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
        sb.append("\n请基于以上来源回答，并在结尾提示“如需更精确答案，请补充更多上下文”。");
        return sb.toString();
    }

    private String parseAnswer(String raw) throws Exception {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        JsonNode root = objectMapper.readTree(raw);

        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (!content.isMissingNode() && !content.isNull()) {
                return content.asText();
            }
        }

        JsonNode outputText = root.path("output").path("text");
        if (!outputText.isMissingNode() && !outputText.isNull()) {
            return outputText.asText();
        }

        return null;
    }
}
