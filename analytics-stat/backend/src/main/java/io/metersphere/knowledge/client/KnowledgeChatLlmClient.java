package io.metersphere.knowledge.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Value("${chat.api.temperature:0.2}")
    private double temperature;

    @Value("${chat.api.max-tokens:600}")
    private int maxTokens;

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

    public String generateAnswer(String question, List<KnowledgeChatSource> sources) {
        if (!available()) {
            return null;
        }

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(Model.of(model))
                    .temperature(temperature)
                    .maxTokens((long) maxTokens)
                    .addUserMessage(buildUserPrompt(question, sources))
                    .build();

            Message message = anthropicClient.messages().create(params);
            return parseAnswer(message);
        } catch (Exception e) {
            logger.warn("LLM 调用失败，降级为模板回答: {}", e.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(String question, List<KnowledgeChatSource> sources) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是企业知识库助手。请仅基于给定引用来源回答，并保持简洁。\n\n");
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
}
