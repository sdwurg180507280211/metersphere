package io.metersphere.base.mapper;

import io.metersphere.knowledge.dto.KbChunkInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文件分片元数据 Mapper
 * 对应表 kb_chunk_info
 */
@Mapper
public interface KbChunkInfoMapper {

    /** 插入分片记录 */
    int insert(KbChunkInfo record);

    /** 查询已上传的分片列表 */
    List<KbChunkInfo> selectByFileMd5AndUserId(@Param("fileMd5") String fileMd5, @Param("userId") String userId);

    /** 根据 fileMd5 和 userId 删除 */
    int deleteByFileMd5AndUserId(@Param("fileMd5") String fileMd5, @Param("userId") String userId);
}
