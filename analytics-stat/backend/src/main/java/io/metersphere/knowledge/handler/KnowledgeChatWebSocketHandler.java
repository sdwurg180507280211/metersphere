package io.metersphere.knowledge.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.knowledge.client.KnowledgeChatLlmClient;
import io.metersphere.knowledge.service.KnowledgeChatWebSocketService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatWebSocketHandler.class);
    private final KnowledgeChatWebSocketService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeChatWebSocketHandler(KnowledgeChatWebSocketService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userToken = extractUserToken(session);
        logger.info("WebSocket连接已建立，token: {}, 会话ID: {}", userToken, session.getId());

        try {
            Map<String, String> connectionMessage = Map.of(
                "type", "connection",
                "sessionId", session.getId()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connectionMessage)));
        } catch (Exception e) {
            logger.error("发送会话ID失败: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userToken = extractUserToken(session);
        try {
            String payload = message.getPayload();
            if (StringUtils.isBlank(payload)) {
                return;
            }

            // 处理心跳
            if ("ping".equalsIgnoreCase(payload.trim())) {
                session.sendMessage(new TextMessage("pong"));
                return;
            }

            logger.info("接收到消息，token: {}, 会话ID: {}, 消息长度: {}", userToken, session.getId(), payload.length());

            Map<String, Object> jsonMessage = objectMapper.readValue(payload, Map.class);
            String action = stringValue(jsonMessage.getOrDefault("action", "ask"));
            String requestId = stringValue(jsonMessage.get("requestId"));

            if ("cancel".equalsIgnoreCase(action)) {
                chatService.cancelRequest(session, requestId);
                return;
            }

            String question = stringValue(jsonMessage.get("question"));
            String mode = stringValue(jsonMessage.getOrDefault("mode", "knowledge"));
            Integer topK = integerValue(jsonMessage.get("topK"), 5);
            String modelId = stringValue(jsonMessage.get("modelId"));
            String workspaceId = stringValue(jsonMessage.get("workspaceId"));
            String userId = stringValue(jsonMessage.get("userId"));
            String conversationId = stringValue(jsonMessage.get("conversationId"));
            List<KnowledgeChatLlmClient.HistoryTurn> history = parseHistory(jsonMessage.get("history"));

            if (StringUtils.isBlank(question)) {
                sendErrorMessage(session, requestId, "问题不能为空");
                return;
            }

            chatService.processMessage(
                userToken,
                userId,
                workspaceId,
                conversationId,
                requestId,
                question.trim(),
                mode,
                topK,
                modelId,
                history,
                session
            );
        } catch (Exception e) {
            logger.error("处理消息出错，token: {}, 会话ID: {}, 错误: {}", userToken, session.getId(), e.getMessage(), e);
            sendErrorMessage(session, null, "消息处理失败：" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatService.cancelSessionRequests(session);
        String userToken = extractUserToken(session);
        logger.info("WebSocket连接已关闭，token: {}, 会话ID: {}, 状态: {}", userToken, session.getId(), status);
    }

    private String extractUserToken(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] segments = path.split("/");
        return segments[segments.length - 1];
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && StringUtils.isNumeric(str)) {
            return Integer.parseInt(str);
        }
        return defaultValue;
    }

    private List<KnowledgeChatLlmClient.HistoryTurn> parseHistory(Object value) {
        List<KnowledgeChatLlmClient.HistoryTurn> history = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return history;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String userQuestion = stringValue(map.get("userQuestion"));
            String assistantAnswer = stringValue(map.get("assistantAnswer"));
            if (StringUtils.isBlank(userQuestion) && StringUtils.isBlank(assistantAnswer)) {
                continue;
            }
            history.add(new KnowledgeChatLlmClient.HistoryTurn(userQuestion, assistantAnswer));
        }
        return history;
    }

    private void sendErrorMessage(WebSocketSession session, String requestId, String errorMessage) {
        try {
            if (StringUtils.isNotBlank(requestId)) {
                Map<String, String> error = Map.of("type", "error", "requestId", requestId, "message", errorMessage);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
                return;
            }
            Map<String, String> error = Map.of("type", "error", "message", errorMessage);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            logger.error("发送错误消息失败: {}", e.getMessage(), e);
        }
    }
}
