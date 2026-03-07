package io.metersphere.knowledge.controller;

import io.metersphere.knowledge.client.KnowledgeChatLlmClient;
import io.metersphere.knowledge.dto.KnowledgeChatStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识问答 Controller
 */
@RestController
@RequestMapping("/knowledge/chat")
public class KnowledgeChatController {

    @Autowired
    private KnowledgeChatLlmClient llmClient;

    /**
     * 问答状态接口
     *
     * URL: GET /knowledge/chat/status
     */
    @GetMapping("/status")
    public KnowledgeChatStatusResponse status() {
        return new KnowledgeChatStatusResponse(llmClient.available());
    }

    /**
     * 获取可用模型列表
     *
     * URL: GET /knowledge/chat/models
     */
    @GetMapping("/models")
    public List<Map<String, String>> listModels() {
        return llmClient.listModels();
    }
}
