package io.metersphere.knowledge.controller;

import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.knowledge.dto.KnowledgeSearchResult;
import io.metersphere.knowledge.service.KnowledgeSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库检索 Controller
 * 提供混合检索接口（KNN 向量召回 + BM25 Rescore）
 *
 * 从 PaiSmart SearchController 迁移，适配 MeterSphere 认证体系：
 * - 用户信息通过 SessionUtils 从 Shiro Session 获取（Gateway 统一认证后透传）
 * - 不再使用 @RequestAttribute("userId")
 */
@RestController
@RequestMapping("/knowledge/search")
public class KnowledgeSearchController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSearchController.class);

    @Autowired
    private KnowledgeSearchService searchService;

    /**
     * 混合检索接口
     *
     * URL: GET /knowledge/search/hybrid?query=xxx&topK=10
     *
     * 响应格式（经 SDK ResultResponseBodyAdvice 自动包装）:
     * {
     *   "success": true,
     *   "message": null,
     *   "data": [{ fileMd5, chunkId, textContent, score, fileName, ... }]
     * }
     *
     * 注意：不要手动构造 { code, message, data } 格式的 Map，
     * SDK 的 ResultResponseBodyAdvice 会自动将返回值包装为 ResultHolder。
     * 如果返回 Map，会被二次包装成 { success: true, data: { code: 200, data: [...] } }，
     * 导致前端解析失败。
     *
     * @param query 搜索关键词（必填）
     * @param topK  返回结果数量（默认10）
     */
    @GetMapping("/hybrid")
    public List<KnowledgeSearchResult> hybridSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK) {

        // 从 Shiro Session 获取当前登录用户信息
        String userId = SessionUtils.getUserId();
        String workspaceId = SessionUtils.getCurrentWorkspaceId();

        logger.info("知识库混合检索 - 用户: {}, 工作空间: {}, 查询: {}, topK: {}",
                userId, workspaceId, query, topK);

        List<KnowledgeSearchResult> results;

        if (userId != null) {
            // 已登录用户：带权限检索
            results = searchService.searchWithPermission(query, userId, workspaceId, topK);
        } else {
            // 未登录（理论上不会走到这里，Gateway 会拦截）：无权限检索
            logger.warn("未获取到用户信息，使用无权限检索");
            results = searchService.search(query, topK);
        }

        logger.info("检索完成，返回 {} 条结果", results.size());

        // 直接返回 List，SDK 的 ResultResponseBodyAdvice 会自动包装为：
        // { "success": true, "message": null, "data": results }
        return results;
    }
}
