package io.metersphere.knowledge.service;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import io.metersphere.base.mapper.KbDocumentVectorMapper;
import io.metersphere.knowledge.dto.KbDocumentVector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文件解析服务
 * 从 PaiSmart ParseService 迁移，适配 MeterSphere MyBatis 体系
 *
 * 核心策略：流式"父文档-子切片"解析
 * 1. Tika AutoDetectParser 提取文件文本内容
 * 2. StreamingContentHandler 按 parentChunkSize 累积文本
 * 3. 累积到阈值后，按语义切分为子切片（段落→句子→HanLP分词）
 * 4. 子切片写入 kb_document_vectors 表
 *
 * 注意：userId/workspaceId 由 Controller 层通过 SessionUtils 获取后传入
 */
@Service
public class KnowledgeParseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeParseService.class);

    @Autowired
    private KbDocumentVectorMapper documentVectorMapper;

    @Value("${file.parsing.chunk-size:512}")
    private int chunkSize;

    @Value("${file.parsing.parent-chunk-size:1048576}")
    private int parentChunkSize;

    @Value("${file.parsing.buffer-size:8192}")
    private int bufferSize;

    @Value("${file.parsing.max-memory-threshold:0.8}")
    private double maxMemoryThreshold;

    /**
     * 流式解析文件并将分块保存到数据库
     *
     * @param fileMd5     文件 MD5 指纹
     * @param fileStream  文件输入流
     * @param userId      上传用户 ID（由 Controller 传入）
     * @param workspaceId 工作空间 ID（由 Controller 传入）
     * @param isPublic    是否公开
     */
    public void parseAndSave(String fileMd5, InputStream fileStream,
                             String userId, String workspaceId, boolean isPublic)
            throws IOException, TikaException {
        logger.info("开始流式解析文件，fileMd5: {}, userId: {}, workspaceId: {}", fileMd5, userId, workspaceId);
        checkMemoryThreshold();

        try (BufferedInputStream bufferedStream = new BufferedInputStream(fileStream, bufferSize)) {
            StreamingContentHandler handler = new StreamingContentHandler(fileMd5, userId, workspaceId, isPublic);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(bufferedStream, handler, metadata, context);
            logger.info("文件流式解析完成，fileMd5: {}, 总分块数: {}", fileMd5, handler.savedChunkCount);
        } catch (SAXException e) {
            logger.error("文档解析失败，fileMd5: {}", fileMd5, e);
            throw new RuntimeException("文档解析失败", e);
        }
    }

    /** 检查内存使用率，超过阈值时触发 GC 或抛出异常 */
    private void checkMemoryThreshold() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsage = (double) usedMemory / runtime.maxMemory();
        if (memoryUsage > maxMemoryThreshold) {
            logger.warn("内存使用率过高: {}, 触发 GC", String.format("%.2f%%", memoryUsage * 100));
            System.gc();
            usedMemory = runtime.totalMemory() - runtime.freeMemory();
            memoryUsage = (double) usedMemory / runtime.maxMemory();
            if (memoryUsage > maxMemoryThreshold) {
                throw new RuntimeException("内存不足，当前使用率: " + String.format("%.2f%%", memoryUsage * 100));
            }
        }
    }

    /**
     * 内部流式内容处理器
     * Tika 解析器调用 characters() 推送文本，累积到 parentChunkSize 后触发切分和入库
     */
    private class StreamingContentHandler extends BodyContentHandler {
        private final StringBuilder buffer = new StringBuilder();
        private final String fileMd5;
        private final String userId;
        private final String workspaceId;
        private final boolean isPublic;
        private int savedChunkCount = 0;

        public StreamingContentHandler(String fileMd5, String userId, String workspaceId, boolean isPublic) {
            super(-1); // 禁用 Tika 内部写入限制
            this.fileMd5 = fileMd5;
            this.userId = userId;
            this.workspaceId = workspaceId;
            this.isPublic = isPublic;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            buffer.append(ch, start, length);
            if (buffer.length() >= parentChunkSize) {
                processParentChunk();
            }
        }

        @Override
        public void endDocument() {
            if (buffer.length() > 0) {
                processParentChunk();
            }
        }

        private void processParentChunk() {
            String parentText = buffer.toString();
            List<String> childChunks = splitTextIntoChunksWithSemantics(parentText, chunkSize);
            
            // 批量插入，减少数据库往返
            List<KbDocumentVector> vectors = new ArrayList<>(childChunks.size());
            for (String chunk : childChunks) {
                savedChunkCount++;
                KbDocumentVector vector = new KbDocumentVector();
                vector.setFileMd5(fileMd5);
                vector.setChunkId(savedChunkCount);
                vector.setTextContent(chunk);
                vector.setUserId(userId);
                vector.setWorkspaceId(workspaceId);
                vector.setPublic(isPublic);
                vectors.add(vector);
            }
            
            if (!vectors.isEmpty()) {
                documentVectorMapper.insertBatch(vectors);
                logger.debug("批量保存 {} 个子切片，累计: {}", vectors.size(), savedChunkCount);
            }
            buffer.setLength(0);
        }
    }

    // ========== 文本切分算法 ==========

    /** 智能文本分割：段落→句子→HanLP分词，保持语义完整性 */
    private List<String> splitTextIntoChunksWithSemantics(String text, int maxSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        StringBuilder cur = new StringBuilder();
        for (String p : paragraphs) {
            if (p.length() > maxSize) {
                if (cur.length() > 0) { chunks.add(cur.toString().trim()); cur = new StringBuilder(); }
                chunks.addAll(splitLongParagraph(p, maxSize));
            } else if (cur.length() + p.length() > maxSize) {
                if (cur.length() > 0) { chunks.add(cur.toString().trim()); }
                cur = new StringBuilder(p);
            } else {
                if (cur.length() > 0) cur.append("\n\n");
                cur.append(p);
            }
        }
        if (cur.length() > 0) chunks.add(cur.toString().trim());
        return chunks;
    }

    /** 按句子边界分割长段落 */
    private List<String> splitLongParagraph(String paragraph, int maxSize) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = paragraph.split("(?<=[。！？；])|(?<=[.!?;])\\s+");
        StringBuilder cur = new StringBuilder();
        for (String s : sentences) {
            if (cur.length() + s.length() > maxSize) {
                if (cur.length() > 0) { chunks.add(cur.toString().trim()); cur = new StringBuilder(); }
                if (s.length() > maxSize) { chunks.addAll(splitLongSentence(s, maxSize)); }
                else { cur.append(s); }
            } else { cur.append(s); }
        }
        if (cur.length() > 0) chunks.add(cur.toString().trim());
        return chunks;
    }

    /** 使用 HanLP 智能分割超长句子，异常时降级为字符切割 */
    private List<String> splitLongSentence(String sentence, int maxSize) {
        List<String> chunks = new ArrayList<>();
        try {
            List<Term> terms = StandardTokenizer.segment(sentence);
            StringBuilder cur = new StringBuilder();
            for (Term term : terms) {
                if (cur.length() + term.word.length() > maxSize && !cur.isEmpty()) {
                    chunks.add(cur.toString()); cur = new StringBuilder();
                }
                cur.append(term.word);
            }
            if (!cur.isEmpty()) chunks.add(cur.toString());
        } catch (Exception e) {
            logger.warn("HanLP 分词异常，降级为字符分割: {}", e.getMessage());
            for (int i = 0; i < sentence.length(); i += maxSize) {
                chunks.add(sentence.substring(i, Math.min(i + maxSize, sentence.length())));
            }
        }
        return chunks;
    }
}
