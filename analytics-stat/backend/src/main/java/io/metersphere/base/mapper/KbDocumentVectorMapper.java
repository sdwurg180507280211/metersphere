package io.metersphere.base.mapper;

import io.metersphere.knowledge.dto.KbDocumentVector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文本分块 Mapper
 * 对应表 kb_document_vectors
 */
@Mapper
public interface KbDocumentVectorMapper {

    /** 插入文本分块记录 */
    int insert(KbDocumentVector record);

    /** 根据 fileMd5 查询所有分块 */
    List<KbDocumentVector> selectByFileMd5(@Param("fileMd5") String fileMd5);

    /** 根据 fileMd5 删除所有分块 */
    int deleteByFileMd5(@Param("fileMd5") String fileMd5);
}
