package io.metersphere.base.mapper;

import io.metersphere.knowledge.dto.KbFileUpload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文件上传记录 Mapper
 * 对应表 kb_file_upload
 */
@Mapper
public interface KbFileUploadMapper {

    /** 插入文件上传记录 */
    int insert(KbFileUpload record);

    /** 根据 fileMd5 和 userId 查询 */
    KbFileUpload selectByFileMd5AndUserId(@Param("fileMd5") String fileMd5, @Param("userId") String userId);

    /** 根据 fileMd5 查询（不限用户） */
    KbFileUpload selectByFileMd5(@Param("fileMd5") String fileMd5);

    /** 根据 fileMd5 列表批量查询（用于检索结果补充文件名） */
    List<KbFileUpload> selectByFileMd5List(@Param("md5List") List<String> md5List);

    /** 更新状态 */
    int updateStatus(@Param("id") Long id, @Param("status") int status);

    /** 查询用户的文件列表 */
    List<KbFileUpload> selectByUserId(@Param("userId") String userId);

    /** 查询工作空间内的文件列表（含公开文件） */
    List<KbFileUpload> selectByWorkspaceId(@Param("workspaceId") String workspaceId, @Param("userId") String userId);

    /** 根据ID删除 */
    int deleteById(@Param("id") Long id);

    /** 根据MD5查询（用于去重） */
    KbFileUpload selectByMd5(@Param("fileMd5") String fileMd5);

    /** 根据主键查询 */
    KbFileUpload selectByPrimaryKey(@Param("id") Long id);

    /** 根据主键删除 */
    int deleteByPrimaryKey(@Param("id") Long id);

    /** 查询用户在工作空间的文件列表 */
    List<KbFileUpload> selectByUserAndWorkspace(@Param("userId") String userId, @Param("workspaceId") String workspaceId);
}
