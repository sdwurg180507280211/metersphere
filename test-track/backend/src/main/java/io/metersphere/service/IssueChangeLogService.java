package io.metersphere.service;

import io.metersphere.base.domain.IssueChangeLog;
import io.metersphere.base.domain.IssueChangeLogDetail;
import io.metersphere.base.mapper.IssueChangeLogDetailMapper;
import io.metersphere.base.mapper.IssueChangeLogMapper;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.dto.IssueChangeLogDTO;
import io.metersphere.dto.IssueChangeLogDetailDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class IssueChangeLogService {

    public static final String SOURCE_UPDATE = "update";
    public static final String SOURCE_STATUS = "status";

    public static final String FIELD_TYPE_SYSTEM = "system";
    public static final String FIELD_TYPE_CUSTOM = "custom";

    @Resource
    private IssueChangeLogMapper issueChangeLogMapper;

    @Resource
    private IssueChangeLogDetailMapper issueChangeLogDetailMapper;

    public List<IssueChangeLogDTO> getHistory(String issueId) {
        if (StringUtils.isBlank(issueId)) {
            return new ArrayList<>();
        }
        List<IssueChangeLog> logs = issueChangeLogMapper.selectByIssueId(issueId);
        if (CollectionUtils.isEmpty(logs)) {
            return new ArrayList<>();
        }

        List<String> logIds = logs.stream().map(IssueChangeLog::getId).collect(Collectors.toList());
        List<IssueChangeLogDetail> details = issueChangeLogDetailMapper.selectByLogIds(logIds);

        Map<String, List<IssueChangeLogDetailDTO>> detailMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(details)) {
            detailMap = details.stream().collect(Collectors.groupingBy(IssueChangeLogDetail::getLogId,
                    Collectors.mapping(this::toDetailDTO, Collectors.toList())));
        }

        List<IssueChangeLogDTO> result = new ArrayList<>(logs.size());
        for (IssueChangeLog log : logs) {
            IssueChangeLogDTO dto = new IssueChangeLogDTO();
            dto.setId(log.getId());
            dto.setIssueId(log.getIssueId());
            dto.setProjectId(log.getProjectId());
            dto.setOperator(log.getOperator());
            dto.setOperatorName(log.getOperatorName());
            dto.setSource(log.getSource());
            dto.setCreateTime(log.getCreateTime());
            dto.setDetails(detailMap.getOrDefault(log.getId(), new ArrayList<>()));
            result.add(dto);
        }
        return result;
    }

    public String saveLog(String issueId, String projectId, String source, List<IssueChangeLogDetailDTO> details) {
        if (StringUtils.isBlank(issueId) || CollectionUtils.isEmpty(details)) {
            return null;
        }
        SessionUser user = SessionUtils.getUser();
        if (user == null) {
            return null;
        }

        String logId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        IssueChangeLog log = new IssueChangeLog();
        log.setId(logId);
        log.setIssueId(issueId);
        log.setProjectId(projectId);
        log.setOperator(user.getId());
        log.setOperatorName(user.getName());
        log.setSource(source);
        log.setCreateTime(now);
        issueChangeLogMapper.insert(log);

        for (IssueChangeLogDetailDTO dto : details) {
            IssueChangeLogDetail d = new IssueChangeLogDetail();
            d.setId(UUID.randomUUID().toString());
            d.setLogId(logId);
            d.setFieldType(dto.getFieldType());
            d.setFieldKey(dto.getFieldKey());
            d.setFieldId(dto.getFieldId());
            d.setFieldName(dto.getFieldName());
            d.setOldValue(dto.getOldValue());
            d.setNewValue(dto.getNewValue());
            d.setCreateTime(now);
            issueChangeLogDetailMapper.insert(d);
        }

        return logId;
    }

    private IssueChangeLogDetailDTO toDetailDTO(IssueChangeLogDetail d) {
        IssueChangeLogDetailDTO dto = new IssueChangeLogDetailDTO();
        dto.setFieldType(d.getFieldType());
        dto.setFieldKey(d.getFieldKey());
        dto.setFieldId(d.getFieldId());
        dto.setFieldName(d.getFieldName());
        dto.setOldValue(d.getOldValue());
        dto.setNewValue(d.getNewValue());
        return dto;
    }
}
