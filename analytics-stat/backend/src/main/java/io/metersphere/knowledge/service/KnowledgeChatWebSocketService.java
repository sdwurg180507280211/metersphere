package io.metersphere.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.knowledge.client.KnowledgeChatLlmClient;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import io.metersphere.knowledge.dto.KnowledgeSearchResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class KnowledgeChatWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatWebSocketService.class);
    private static final int MAX_SNIPPET_LENGTH = 200;
    private static final int MAX_TOP_K = 10;
    private static final int MAX_HISTORY_TURNS_LIMIT = 20;

    @Value("${chat.api.top-k:5}")
    private int defaultTopK;

    @Value("${chat.api.history-turns:5}")
    private int historyTurns;

    @Autowired
    private KnowledgeSearchService searchService;

    @Autowired
    private KnowledgeChatLlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, CompletableFuture<Void>> activeRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> cancelledRequests = new ConcurrentHashMap<>();

    public void processMessage(String fallbackUserId,
                               String userId,
                               String workspaceId,
                               String conversationId,
                               String requestId,
                               String question,
                               String mode,
                               Integer topK,
                               String modelId,
                               List<KnowledgeChatLlmClient.HistoryTurn> history,
                               WebSocketSession session) {
        String normalizedRequestId = StringUtils.defaultIfBlank(requestId, session.getId() + "-" + System.currentTimeMillis());
        String taskKey = buildTaskKey(session, normalizedRequestId);
        String effectiveUserId = StringUtils.defaultIfBlank(userId, fallbackUserId);
        String effectiveWorkspaceId = StringUtils.defaultIfBlank(workspaceId, "default");
        String effectiveMode = StringUtils.defaultIfBlank(mode, "knowledge");
        int safeTopK = normalizeTopK(topK);
        List<KnowledgeChatLlmClient.HistoryTurn> safeHistory = normalizeHistory(history);

        cancelledRequests.remove(taskKey);
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            try {
                if ("knowledge".equals(effectiveMode)) {
                    processKnowledgeMode(effectiveUserId, effectiveWorkspaceId, normalizedRequestId, question, safeTopK, modelId, safeHistory, session, taskKey);
                } else {
                    processNormalMode(normalizedRequestId, question, modelId, safeHistory, session, taskKey);
                }
                if (!isCancelled(taskKey)) {
                    sendDone(session, normalizedRequestId);
                }
            } catch (CancellationException e) {
                logger.info("聊天请求已取消, sessionId={}, requestId={}", session.getId(), normalizedRequestId);
                sendCancelled(session, normalizedRequestId);
            } catch (Exception e) {
                logger.error("处理消息失败, sessionId={}, requestId={}, error={}", session.getId(), normalizedRequestId, e.getMessage(), e);
                sendError(session, normalizedRequestId, e.getMessage());
            } finally {
                activeRequests.remove(taskKey);
                cancelledRequests.remove(taskKey);
            }
        });

        activeRequests.put(taskKey, task);
    }

    public void cancelRequest(WebSocketSession session, String requestId) {
        if (StringUtils.isBlank(requestId)) {
            return;
        }
        String taskKey = buildTaskKey(session, requestId);
        cancelledRequests.put(taskKey, Boolean.TRUE);
        CompletableFuture<Void> task = activeRequests.remove(taskKey);
        if (task != null) {
            task.cancel(true);
        }
    }

    public void cancelSessionRequests(WebSocketSession session) {
        String prefix = session.getId() + ":";
        activeRequests.keySet().stream()
            .filter(key -> key.startsWith(prefix))
            .collect(Collectors.toList())
            .forEach(key -> {
                cancelledRequests.put(key, Boolean.TRUE);
                CompletableFuture<Void> task = activeRequests.remove(key);
                if (task != null) {
                    task.cancel(true);
                }
            });
    }

    private void processKnowledgeMode(String userId,
                                      String workspaceId,
                                      String requestId,
                                      String question,
                                      int topK,
                                      String modelId,
                                      List<KnowledgeChatLlmClient.HistoryTurn> history,
                                      WebSocketSession session,
                                      String taskKey) {
        List<KnowledgeSearchResult> searchResults = searchService.searchWithPermission(question, userId, workspaceId, topK);
        List<KnowledgeChatSource> sources = toSources(searchResults);

        sendSources(session, requestId, sources);
        throwIfCancelled(taskKey);

        if (sources.isEmpty()) {
            sendDelta(session, requestId, "未检索到与问题相关的知识内容，请尝试缩短关键词或更换提问方式。");
            return;
        }

        String answer = llmClient.streamAnswer(question, sources, history, modelId, () -> isCancelled(taskKey), delta -> sendDelta(session, requestId, delta));
        if (StringUtils.isBlank(answer) && !isCancelled(taskKey)) {
            sendDelta(session, requestId, buildTemplateAnswer(question, sources));
        }
    }

    private void processNormalMode(String requestId,
                                   String question,
                                   String modelId,
                                   List<KnowledgeChatLlmClient.HistoryTurn> history,
                                   WebSocketSession session,
                                   String taskKey) {
        String answer = llmClient.streamNormalChat(question, history, modelId, () -> isCancelled(taskKey), delta -> sendDelta(session, requestId, delta));
        if (StringUtils.isBlank(answer) && !isCancelled(taskKey)) {
            sendDelta(session, requestId, "抱歉，AI 服务暂时不可用，请稍后重试。");
        }
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return defaultTopK;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private List<KnowledgeChatLlmClient.HistoryTurn> normalizeHistory(List<KnowledgeChatLlmClient.HistoryTurn> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        int keepTurns = Math.max(1, Math.min(historyTurns, MAX_HISTORY_TURNS_LIMIT));
        return history.stream()
            .filter(item -> item != null && (StringUtils.isNotBlank(item.userQuestion()) || StringUtils.isNotBlank(item.assistantAnswer())))
            .skip(Math.max(0, history.size() - keepTurns))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private String buildTaskKey(WebSocketSession session, String requestId) {
        return session.getId() + ":" + requestId;
    }

    private boolean isCancelled(String taskKey) {
        return cancelledRequests.containsKey(taskKey) || Thread.currentThread().isInterrupted();
    }

    private void throwIfCancelled(String taskKey) {
        if (isCancelled(taskKey)) {
            throw new CancellationException("request cancelled");
        }
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

    private String buildTemplateAnswer(String question, List<KnowledgeChatSource> sources) {
        List<String> bullets = sources.stream()
            .limit(3)
            .map(source -> String.format("- [%s#%s] %s", source.getFileName(), source.getChunkId(), source.getSnippet()))
            .collect(Collectors.toList());
        return String.format(
            "根据知识库检索结果，问题“%s”的相关要点如下：\n\n%s\n\n请结合引用来源进一步核对细节。",
            question,
            String.join("\n", bullets)
        );
    }

    private void sendDelta(WebSocketSession session, String requestId, String delta) {
        try {
            Map<String, String> message = Map.of("type", "delta", "requestId", requestId, "data", delta);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送delta失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void sendSources(WebSocketSession session, String requestId, List<KnowledgeChatSource> sources) {
        try {
            Map<String, Object> message = Map.of("type", "sources", "requestId", requestId, "data", sources);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送sources失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void sendDone(WebSocketSession session, String requestId) {
        try {
            Map<String, String> message = Map.of("type", "done", "requestId", requestId, "data", "ok");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送done失败: {}", e.getMessage(), e);
        }
    }

    private void sendCancelled(WebSocketSession session, String requestId) {
        try {
            Map<String, String> message = Map.of("type", "cancelled", "requestId", requestId, "data", "cancelled");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送cancelled失败: {}", e.getMessage(), e);
        }
    }

    private void sendError(WebSocketSession session, String requestId, String error) {
        try {
            Map<String, String> message = Map.of("type", "error", "requestId", requestId, "message", StringUtils.defaultString(error, "unknown error"));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送error失败: {}", e.getMessage(), e);
        }
    }
}
