package io.metersphere.knowledge.service;

import io.metersphere.knowledge.client.KnowledgeChatLlmClient;
import io.metersphere.knowledge.dto.KnowledgeChatAskResponse;
import io.metersphere.knowledge.dto.KnowledgeChatSource;
import io.metersphere.knowledge.dto.KnowledgeSearchResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @Autowired
    private KnowledgeSearchService searchService;

    @Autowired
    private KnowledgeChatLlmClient knowledgeChatLlmClient;

    public boolean isLlmAvailable() {
        return knowledgeChatLlmClient.available();
    }

    public KnowledgeChatAskResponse ask(String question, Integer topK, String userId, String workspaceId) {
        return generateResponse(question, topK, userId, workspaceId);
    }

    public SseEmitter askStream(String question, Integer topK, String userId, String workspaceId) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                KnowledgeChatAskResponse response = generateResponse(question, topK, userId, workspaceId);

                emitter.send(SseEmitter.event()
                        .name("sources")
                        .data(response.getSources()));

                for (String chunk : splitTextToChunks(response.getAnswer(), 20)) {
                    emitter.send(SseEmitter.event().name("delta").data(chunk));
                    Thread.sleep(20L);
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

    private KnowledgeChatAskResponse generateResponse(String question, Integer topK, String userId, String workspaceId) {
        int safeTopK = normalizeTopK(topK);
        List<KnowledgeSearchResult> searchResults = searchService.searchWithPermission(
                question, userId, workspaceId, safeTopK
        );

        List<KnowledgeChatSource> sources = toSources(searchResults);
        String answer = buildAnswer(question, sources);

        return new KnowledgeChatAskResponse(answer, sources);
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

    private String buildAnswer(String question, List<KnowledgeChatSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return "未检索到与问题相关的知识内容，请尝试缩短关键词或更换提问方式。";
        }

        String llmAnswer = knowledgeChatLlmClient.generateAnswer(question, sources);
        if (StringUtils.isNotBlank(llmAnswer)) {
            return llmAnswer;
        }

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
}
