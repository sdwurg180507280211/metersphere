package io.metersphere.knowledge.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.knowledge.service.KnowledgeChatWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KnowledgeChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatWebSocketHandler.class);
    private final KnowledgeChatWebSocketService chatService;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeChatWebSocketHandler(KnowledgeChatWebSocketService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        sessions.put(userId, session);
        logger.info("WebSocket连接已建立，用户ID: {}, 会话ID: {}", userId, session.getId());

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
        String userId = extractUserId(session);
        try {
            String payload = message.getPayload();
            logger.info("接收到消息，用户ID: {}, 会话ID: {}, 消息长度: {}", userId, session.getId(), payload.length());

            Map<String, Object> jsonMessage = objectMapper.readValue(payload, Map.class);
            String question = (String) jsonMessage.get("question");
            String mode = (String) jsonMessage.getOrDefault("mode", "knowledge");
            Integer topK = (Integer) jsonMessage.getOrDefault("topK", 5);

            if (question == null || question.trim().isEmpty()) {
                sendErrorMessage(session, "问题不能为空");
                return;
            }

            chatService.processMessage(userId, question.trim(), mode, topK, session);

        } catch (Exception e) {
            logger.error("处理消息出错，用户ID: {}, 会话ID: {}, 错误: {}", userId, session.getId(), e.getMessage(), e);
            sendErrorMessage(session, "消息处理失败：" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = extractUserId(session);
        sessions.remove(userId);
        logger.info("WebSocket连接已关闭，用户ID: {}, 会话ID: {}, 状态: {}", userId, session.getId(), status);
    }

    private String extractUserId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] segments = path.split("/");
        return segments[segments.length - 1];
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            Map<String, String> error = Map.of("type", "error", "message", errorMessage);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            logger.error("发送错误消息失败: {}", e.getMessage(), e);
        }
    }
}
