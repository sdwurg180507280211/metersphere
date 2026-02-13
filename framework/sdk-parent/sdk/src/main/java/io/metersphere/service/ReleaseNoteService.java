package io.metersphere.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.base.domain.ReleaseNote;
import io.metersphere.base.mapper.ReleaseNoteMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.PageUtils;
import io.metersphere.commons.utils.Pager;
import io.metersphere.commons.utils.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 需求上线记录 Service
 * 提供上线记录的增删改查业务逻辑，包括分页查询和最近记录查询
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ReleaseNoteService {

    @Resource
    private ReleaseNoteMapper releaseNoteMapper;

    /**
     * 新增上线记录
     * 自动填充 id（UUID）、creator（当前登录用户）、createTime 和 updateTime
     */
    public ReleaseNote add(ReleaseNote releaseNote) {
        validateRequiredFields(releaseNote);
        releaseNote.setId(UUID.randomUUID().toString());
        releaseNote.setCreator(SessionUtils.getUserId());
        long now = System.currentTimeMillis();
        releaseNote.setCreateTime(now);
        releaseNote.setUpdateTime(now);
        releaseNoteMapper.insert(releaseNote);
        return releaseNote;
    }

    /**
     * 更新上线记录，仅更新非 null 字段，自动刷新 updateTime
     */
    public void update(ReleaseNote releaseNote) {
        checkExist(releaseNote.getId());
        releaseNote.setUpdateTime(System.currentTimeMillis());
        releaseNoteMapper.updateByPrimaryKeySelective(releaseNote);
    }


    /** 删除上线记录 */
    public void delete(String id) {
        checkExist(id);
        releaseNoteMapper.deleteByPrimaryKey(id);
    }

    /** 分页查询上线记录列表（按 create_time 倒序），使用 PageHelper */
    public Pager<List<ReleaseNote>> list(int goPage, int pageSize) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        return PageUtils.setPageInfo(page, releaseNoteMapper.selectAll());
    }

    /** 获取最近 N 条上线记录（供测试跟踪首页展示） */
    public List<ReleaseNote> recent(int limit) {
        return releaseNoteMapper.selectRecent(limit);
    }

    /** 根据 ID 获取单条上线记录，不存在时抛出 MSException */
    public ReleaseNote get(String id) {
        ReleaseNote releaseNote = releaseNoteMapper.selectByPrimaryKey(id);
        if (releaseNote == null) {
            MSException.throwException("上线记录不存在: " + id);
        }
        return releaseNote;
    }

    /** 校验必填字段：title 和 content 不能为空 */
    private void validateRequiredFields(ReleaseNote releaseNote) {
        if (StringUtils.isBlank(releaseNote.getTitle())) {
            MSException.throwException("标题不能为空");
        }
        if (StringUtils.isBlank(releaseNote.getContent())) {
            MSException.throwException("内容不能为空");
        }
    }

    /** 校验记录是否存在 */
    private void checkExist(String id) {
        if (releaseNoteMapper.selectByPrimaryKey(id) == null) {
            MSException.throwException("上线记录不存在: " + id);
        }
    }
}
