package io.metersphere.knowledge.service;

import io.metersphere.knowledge.client.KnowledgeChatLlmClient;
import io.metersphere.knowledge.dto.KnowledgeChatAskResponse;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import io.metersphere.knowledge.dto.KnowledgeSearchResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识问答服务
 *
 * P0 阶段：基于现有混合检索结果拼接回答，保证前后端联调可用。
 */
@Service
public class KnowledgeChatService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;
    private static final int MAX_SNIPPET_LENGTH = 200;
    private static final int MAX_HISTORY_TURNS_LIMIT = 20;

    @Value("${chat.api.history-turns:5}")
    private int historyTurns;

    @Autowired
    private KnowledgeSearchService searchService;

    @Autowired
    private KnowledgeChatLlmClient knowledgeChatLlmClient;

    private final ConcurrentHashMap<String, Deque<KnowledgeChatLlmClient.HistoryTurn>> conversationHistory = new ConcurrentHashMap<>();

    public boolean isLlmAvailable() {
        return knowledgeChatLlmClient.available();
    }

    public KnowledgeChatAskResponse ask(String question, Integer topK, String userId, String workspaceId) {
        int safeTopK = normalizeTopK(topK);
        List<KnowledgeChatSource> sources = loadSources(question, userId, workspaceId, safeTopK);
        String historyKey = buildHistoryKey(userId, workspaceId);
        List<KnowledgeChatLlmClient.HistoryTurn> history = getHistorySnapshot(historyKey);
        String answer = buildAnswer(question, sources, history);
        appendHistory(historyKey, question, answer);
        return new KnowledgeChatAskResponse(answer, sources);
    }

    public SseEmitter askStream(String question, Integer topK, String userId, String workspaceId) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                int safeTopK = normalizeTopK(topK);
                List<KnowledgeChatSource> sources = loadSources(question, userId, workspaceId, safeTopK);
                emitter.send(SseEmitter.event().name("sources").data(sources));

                String answer;
                if (sources.isEmpty()) {
                    answer = "未检索到与问题相关的知识内容，请尝试缩短关键词或更换提问方式。";
                    emitter.send(SseEmitter.event().name("delta").data(answer));
                } else {
                    String historyKey = buildHistoryKey(userId, workspaceId);
                    List<KnowledgeChatLlmClient.HistoryTurn> history = getHistorySnapshot(historyKey);
                    String llmAnswer = knowledgeChatLlmClient.streamAnswer(question, sources, history,
                            delta -> safeSendDelta(emitter, delta));

                    if (StringUtils.isNotBlank(llmAnswer)) {
                        answer = llmAnswer;
                    } else {
                        answer = buildTemplateAnswer(question, sources);
                        for (String chunk : splitTextToChunks(answer, 20)) {
                            emitter.send(SseEmitter.event().name("delta").data(chunk));
                            Thread.sleep(20L);
                        }
                    }
                    appendHistory(historyKey, question, answer);
                }


                emitter.send(SseEmitter.event().name("done").data("ok"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private List<KnowledgeChatSource> loadSources(String question, String userId, String workspaceId, int safeTopK) {
        List<KnowledgeSearchResult> searchResults = searchService.searchWithPermission(
                question, userId, workspaceId, safeTopK
        );
        return toSources(searchResults);
    }

    private void safeSendDelta(SseEmitter emitter, String delta) {
        try {
            emitter.send(SseEmitter.event().name("delta").data(delta));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> splitTextToChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (StringUtils.isBlank(text)) {
            chunks.add("");
            return chunks;
        }

        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
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

    private String buildAnswer(String question, List<KnowledgeChatSource> sources, List<KnowledgeChatLlmClient.HistoryTurn> history) {
        if (sources == null || sources.isEmpty()) {
            return "未检索到与问题相关的知识内容，请尝试缩短关键词或更换提问方式。";
        }

        String llmAnswer = knowledgeChatLlmClient.generateAnswer(question, sources, history);
        if (StringUtils.isNotBlank(llmAnswer)) {
            return llmAnswer;
        }

        return buildTemplateAnswer(question, sources);
    }

    private String buildTemplateAnswer(String question, List<KnowledgeChatSource> sources) {

        List<String> bullets = sources.stream()
                .limit(3)
                .map(source -> String.format("- [%s#%s] %s",
                        source.getFileName(),
                        source.getChunkId(),
                        source.getSnippet()))
                .collect(Collectors.toList());

        return String.format(
                "根据知识库检索结果，问题“%s”的相关要点如下：\n\n%s\n\n请结合引用来源进一步核对细节。",
                question,
                String.join("\n", bullets)
        );
    }

    private String buildHistoryKey(String userId, String workspaceId) {
        return String.format("%s:%s", StringUtils.defaultString(userId, "unknown"), StringUtils.defaultString(workspaceId, "default"));
    }

    private List<KnowledgeChatLlmClient.HistoryTurn> getHistorySnapshot(String historyKey) {
        Deque<KnowledgeChatLlmClient.HistoryTurn> queue = conversationHistory.get(historyKey);
        if (queue == null || queue.isEmpty()) {
            return new ArrayList<>();
        }
        synchronized (queue) {
            return new ArrayList<>(queue);
        }
    }

    private void appendHistory(String historyKey, String question, String answer) {
        if (StringUtils.isBlank(question) || StringUtils.isBlank(answer)) {
            return;
        }
        Deque<KnowledgeChatLlmClient.HistoryTurn> queue = conversationHistory.computeIfAbsent(historyKey, key -> new ArrayDeque<>());
        int keepTurns = Math.max(1, Math.min(historyTurns, MAX_HISTORY_TURNS_LIMIT));
        synchronized (queue) {
            queue.addLast(new KnowledgeChatLlmClient.HistoryTurn(question, answer));
            while (queue.size() > keepTurns) {
                queue.removeFirst();
            }
        }
    }
}
