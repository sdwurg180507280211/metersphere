package io.metersphere.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.knowledge.client.KnowledgeChatLlmClient;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import io.metersphere.knowledge.dto.KnowledgeSearchResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeChatWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatWebSocketService.class);
    private static final int MAX_SNIPPET_LENGTH = 200;

    @Autowired
    private KnowledgeSearchService searchService;

    @Autowired
    private KnowledgeChatLlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processMessage(String userId, String question, String mode, Integer topK, WebSocketSession session) {
        try {
            if ("knowledge".equals(mode)) {
                processKnowledgeMode(userId, question, topK, session);
            } else {
                processNormalMode(userId, question, session);
            }
        } catch (Exception e) {
            logger.error("处理消息失败: {}", e.getMessage(), e);
            sendError(session, e.getMessage());
        }
    }

    private void processKnowledgeMode(String userId, String question, Integer topK, WebSocketSession session) {
        List<KnowledgeSearchResult> searchResults = searchService.searchWithPermission(question, userId, "default", topK);
        List<KnowledgeChatSource> sources = toSources(searchResults);

        sendSources(session, sources);

        if (sources.isEmpty()) {
            sendDelta(session, "未检索到与问题相关的知识内容，请尝试缩短关键词或更换提问方式。");
            sendDone(session);
            return;
        }

        llmClient.streamAnswer(question, sources, new ArrayList<>(), delta -> sendDelta(session, delta));
        sendDone(session);
    }

    private void processNormalMode(String userId, String question, WebSocketSession session) {
        llmClient.streamNormalChat(question, new ArrayList<>(), delta -> sendDelta(session, delta));
        sendDone(session);
    }

    private List<KnowledgeChatSource> toSources(List<KnowledgeSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        return results.stream().map(item -> {
            String fileName = StringUtils.defaultIfBlank(item.getFileName(), item.getFileMd5());
            String snippet = StringUtils.defaultString(item.getTextContent());
            if (snippet.length() > MAX_SNIPPET_LENGTH) {
                snippet = snippet.substring(0, MAX_SNIPPET_LENGTH) + "...";
            }
            return new KnowledgeChatSource(fileName, item.getChunkId(), snippet, item.getScore());
        }).collect(Collectors.toList());
    }

    private void sendDelta(WebSocketSession session, String delta) {
        try {
            Map<String, String> message = Map.of("type", "delta", "data", delta);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送delta失败: {}", e.getMessage(), e);
        }
    }

    private void sendSources(WebSocketSession session, List<KnowledgeChatSource> sources) {
        try {
            Map<String, Object> message = Map.of("type", "sources", "data", sources);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送sources失败: {}", e.getMessage(), e);
        }
    }

    private void sendDone(WebSocketSession session) {
        try {
            Map<String, String> message = Map.of("type", "done", "data", "ok");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送done失败: {}", e.getMessage(), e);
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            Map<String, String> message = Map.of("type", "error", "message", error);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送error失败: {}", e.getMessage(), e);
        }
    }
}
