package io.metersphere.knowledge.controller;

import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.knowledge.dto.KnowledgeChatAskRequest;
import io.metersphere.knowledge.dto.KnowledgeChatAskResponse;
import io.metersphere.knowledge.dto.KnowledgeChatStatusResponse;
import io.metersphere.knowledge.service.KnowledgeChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 知识问答 Controller
 */
@RestController
@RequestMapping("/knowledge/chat")
public class KnowledgeChatController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeChatController.class);

    @Autowired
    private KnowledgeChatService knowledgeChatService;

    /**
     * 问答接口（P0）
     *
     * URL: POST /knowledge/chat/ask
     */
    @PostMapping("/ask")
    public KnowledgeChatAskResponse ask(@RequestBody KnowledgeChatAskRequest request) {
        String userId = SessionUtils.getUserId();
        String workspaceId = SessionUtils.getCurrentWorkspaceId();
        String question = validateQuestion(request);

        logger.info("知识问答请求 - 用户: {}, 工作空间: {}, questionLength: {}, topK: {}",
                userId, workspaceId, question.length(), request.getTopK());

        return knowledgeChatService.ask(question.trim(), request.getTopK(), userId, workspaceId);
    }

    /**
     * 流式问答接口（P0.5）
     *
     * URL: POST /knowledge/chat/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody KnowledgeChatAskRequest request, HttpServletResponse response) {
        String userId = SessionUtils.getUserId();
        String workspaceId = SessionUtils.getCurrentWorkspaceId();
        String question = validateQuestion(request);

        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        logger.info("知识流式问答请求 - 用户: {}, 工作空间: {}, questionLength: {}, topK: {}",
                userId, workspaceId, question.length(), request.getTopK());

        return knowledgeChatService.askStream(question.trim(), request.getTopK(), userId, workspaceId);
    }

    /**
     * 普通对话流式接口（不使用 RAG）
     *
     * URL: POST /knowledge/chat/normal-stream
     */
    @PostMapping(value = "/normal-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter normalStream(@RequestBody KnowledgeChatAskRequest request, HttpServletResponse response) {
        String userId = SessionUtils.getUserId();
        String workspaceId = SessionUtils.getCurrentWorkspaceId();
        String question = validateQuestion(request);

        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        logger.info("普通对话流式请求 - 用户: {}, 工作空间: {}, questionLength: {}",
                userId, workspaceId, question.length());

        return knowledgeChatService.askNormalStream(question.trim(), userId, workspaceId);
    }

    /**
     * 问答状态接口
     *
     * URL: GET /knowledge/chat/status
     */
    @GetMapping("/status")
    public KnowledgeChatStatusResponse status() {
        return new KnowledgeChatStatusResponse(knowledgeChatService.isLlmAvailable());
    }

    private String validateQuestion(KnowledgeChatAskRequest request) {
        String question = request == null ? null : request.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            MSException.throwException("question 不能为空");
        }
        return question;
    }
}
