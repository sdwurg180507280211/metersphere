# 用户ID批量修改实施方案

## 一、背景说明

生产环境300+用户ID命名不规范，需要批量修改为规范格式。

## 二、ID命名规范

### 当前问题示例
- 不规范格式：`user123`, `test_user`, `张三`, `admin001`

### 目标规范格式
建议采用以下格式之一：
1. **UUID格式**：`550e8400-e29b-41d4-a716-446655440000`（推荐）
2. **前缀+序号**：`USR_000001`, `USR_000002`
3. **邮箱前缀**：`zhangsan@company.com` → `zhangsan`

## 三、完整SQL脚本

### 3.1 准备阶段

```sql
-- ========================================
-- 第一步：全量备份（在执行前必须完成）
-- ========================================
-- 在宿主机执行：
-- docker exec mysql mysqldump -uroot -p'Password123@mysql' metersphere_dev > backup_before_user_id_change_$(date +%Y%m%d_%H%M%S).sql

-- ========================================
-- 第二步：创建ID映射表
-- ========================================
USE metersphere_dev;

-- 创建映射表
DROP TABLE IF EXISTS user_id_mapping;
CREATE TABLE user_id_mapping (
    old_id VARCHAR(50) NOT NULL COMMENT '原用户ID',
    new_id VARCHAR(50) NOT NULL COMMENT '新用户ID',
    user_name VARCHAR(64) COMMENT '用户名（便于核对）',
    user_email VARCHAR(64) COMMENT '用户邮箱（便于核对）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (old_id),
    UNIQUE KEY uk_new_id (new_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户ID映射表';

-- ========================================
-- 第三步：生成新ID映射（根据实际规则调整）
-- ========================================

-- 方案1：使用UUID（推荐）
INSERT INTO user_id_mapping (old_id, new_id, user_name, user_email)
SELECT 
    id as old_id,
    REPLACE(UUID(), '-', '') as new_id,  -- 生成32位UUID（去掉横线）
    name as user_name,
    email as user_email
FROM user;

-- 方案2：使用前缀+序号
-- INSERT INTO user_id_mapping (old_id, new_id, user_name, user_email)
-- SELECT 
--     id as old_id,
--     CONCAT('USR_', LPAD(@rownum := @rownum + 1, 6, '0')) as new_id,
--     name as user_name,
--     email as user_email
-- FROM user, (SELECT @rownum := 0) r
-- ORDER BY create_time;

-- 方案3：使用邮箱前缀
-- INSERT INTO user_id_mapping (old_id, new_id, user_name, user_email)
-- SELECT 
--     id as old_id,
--     SUBSTRING_INDEX(email, '@', 1) as new_id,
--     name as user_name,
--     email as user_email
-- FROM user;

-- ========================================
-- 第四步：导出映射表供人工审核
-- ========================================
-- SELECT * FROM user_id_mapping ORDER BY old_id;
-- 导出到CSV：
-- SELECT old_id, new_id, user_name, user_email 
-- FROM user_id_mapping 
-- INTO OUTFILE '/tmp/user_id_mapping.csv'
-- FIELDS TERMINATED BY ',' 
-- ENCLOSED BY '"'
-- LINES TERMINATED BY '\n';
```

### 3.2 执行阶段（停机维护窗口）

```sql
-- ========================================
-- 执行前检查清单
-- ========================================
-- [ ] 1. 已完成数据库全量备份
-- [ ] 2. 已生成并审核user_id_mapping表
-- [ ] 3. 已停止所有微服务
-- [ ] 4. 已通知所有用户系统维护
-- [ ] 5. 已准备回滚脚本
-- [ ] 6. 已在测试环境验证通过

-- ========================================
-- 开始执行（按顺序执行，不可跳过）
-- ========================================

USE metersphere_dev;

-- 关闭外键检查（重要！）
SET FOREIGN_KEY_CHECKS = 0;

-- 开启事务（可选，但建议分批执行）
START TRANSACTION;

-- ========================================
-- 更新所有关联表（按依赖顺序）
-- ========================================

-- 1. 权限相关表（最重要）
UPDATE user_group ug
INNER JOIN user_id_mapping m ON ug.user_id = m.old_id
SET ug.user_id = m.new_id;

UPDATE user_role ur
INNER JOIN user_id_mapping m ON ur.user_id = m.old_id
SET ur.user_id = m.new_id;

-- 2. 用户个人数据表
UPDATE user_key uk
INNER JOIN user_id_mapping m ON uk.user_id = m.old_id
SET uk.user_id = m.new_id;

UPDATE user_header uh
INNER JOIN user_id_mapping m ON uh.user_id = m.old_id
SET uh.user_id = m.new_id;

-- 3. 测试跟踪模块
UPDATE test_case tc
INNER JOIN user_id_mapping m ON tc.maintainer = m.old_id
SET tc.maintainer = m.new_id
WHERE tc.maintainer IS NOT NULL;

UPDATE test_case tc
INNER JOIN user_id_mapping m ON tc.create_user = m.old_id
SET tc.create_user = m.new_id
WHERE tc.create_user IS NOT NULL;

UPDATE test_case tc
INNER JOIN user_id_mapping m ON tc.delete_user_id = m.old_id
SET tc.delete_user_id = m.new_id
WHERE tc.delete_user_id IS NOT NULL;

UPDATE test_case_review_users tcru
INNER JOIN user_id_mapping m ON tcru.user_id = m.old_id
SET tcru.user_id = m.new_id;

UPDATE test_case_review_test_case_users trcu
INNER JOIN user_id_mapping m ON trcu.user_id = m.old_id
SET trcu.user_id = m.new_id;

UPDATE test_case_review tcr
INNER JOIN user_id_mapping m ON tcr.create_user = m.old_id
SET tcr.create_user = m.new_id
WHERE tcr.create_user IS NOT NULL;

UPDATE test_case_review_test_case trct
INNER JOIN user_id_mapping m ON trct.create_user = m.old_id
SET trct.create_user = m.new_id
WHERE trct.create_user IS NOT NULL;

UPDATE test_case_review_test_case trct
INNER JOIN user_id_mapping m ON trct.reviewer = m.old_id
SET trct.reviewer = m.new_id
WHERE trct.reviewer IS NOT NULL;

UPDATE test_plan_test_case tptc
INNER JOIN user_id_mapping m ON tptc.executor = m.old_id
SET tptc.executor = m.new_id
WHERE tptc.executor IS NOT NULL;

UPDATE test_plan_test_case tptc
INNER JOIN user_id_mapping m ON tptc.create_user = m.old_id
SET tptc.create_user = m.new_id
WHERE tptc.create_user IS NOT NULL;

-- 4. 接口测试模块
UPDATE api_definition ad
INNER JOIN user_id_mapping m ON ad.user_id = m.old_id
SET ad.user_id = m.new_id
WHERE ad.user_id IS NOT NULL;

UPDATE api_definition ad
INNER JOIN user_id_mapping m ON ad.create_user = m.old_id
SET ad.create_user = m.new_id
WHERE ad.create_user IS NOT NULL;

UPDATE api_definition ad
INNER JOIN user_id_mapping m ON ad.delete_user_id = m.old_id
SET ad.delete_user_id = m.new_id
WHERE ad.delete_user_id IS NOT NULL;

UPDATE api_scenario aps
INNER JOIN user_id_mapping m ON aps.user_id = m.old_id
SET aps.user_id = m.new_id
WHERE aps.user_id IS NOT NULL;

UPDATE api_scenario aps
INNER JOIN user_id_mapping m ON aps.create_user = m.old_id
SET aps.create_user = m.new_id
WHERE aps.create_user IS NOT NULL;

UPDATE api_scenario aps
INNER JOIN user_id_mapping m ON aps.principal = m.old_id
SET aps.principal = m.new_id
WHERE aps.principal IS NOT NULL;

UPDATE api_scenario aps
INNER JOIN user_id_mapping m ON aps.delete_user_id = m.old_id
SET aps.delete_user_id = m.new_id
WHERE aps.delete_user_id IS NOT NULL;

UPDATE api_test_case atc
INNER JOIN user_id_mapping m ON atc.create_user_id = m.old_id
SET atc.create_user_id = m.new_id
WHERE atc.create_user_id IS NOT NULL;

UPDATE api_test_case atc
INNER JOIN user_id_mapping m ON atc.update_user_id = m.old_id
SET atc.update_user_id = m.new_id
WHERE atc.update_user_id IS NOT NULL;

UPDATE api_test_case atc
INNER JOIN user_id_mapping m ON atc.delete_user_id = m.old_id
SET atc.delete_user_id = m.new_id
WHERE atc.delete_user_id IS NOT NULL;

UPDATE api_scenario_report asr
INNER JOIN user_id_mapping m ON asr.user_id = m.old_id
SET asr.user_id = m.new_id
WHERE asr.user_id IS NOT NULL;

UPDATE api_scenario_report asr
INNER JOIN user_id_mapping m ON asr.create_user = m.old_id
SET asr.create_user = m.new_id
WHERE asr.create_user IS NOT NULL;

-- 5. 性能测试模块
UPDATE load_test lt
INNER JOIN user_id_mapping m ON lt.user_id = m.old_id
SET lt.user_id = m.new_id
WHERE lt.user_id IS NOT NULL;

UPDATE load_test lt
INNER JOIN user_id_mapping m ON lt.create_user = m.old_id
SET lt.create_user = m.new_id
WHERE lt.create_user IS NOT NULL;

UPDATE load_test_report ltr
INNER JOIN user_id_mapping m ON ltr.user_id = m.old_id
SET ltr.user_id = m.new_id
WHERE ltr.user_id IS NOT NULL;

-- 6. UI测试模块
UPDATE ui_scenario us
INNER JOIN user_id_mapping m ON us.user_id = m.old_id
SET us.user_id = m.new_id
WHERE us.user_id IS NOT NULL;

UPDATE ui_scenario us
INNER JOIN user_id_mapping m ON us.create_user = m.old_id
SET us.create_user = m.new_id
WHERE us.create_user IS NOT NULL;

UPDATE ui_scenario us
INNER JOIN user_id_mapping m ON us.principal = m.old_id
SET us.principal = m.new_id
WHERE us.principal IS NOT NULL;

UPDATE ui_scenario us
INNER JOIN user_id_mapping m ON us.delete_user_id = m.old_id
SET us.delete_user_id = m.new_id
WHERE us.delete_user_id IS NOT NULL;

-- 7. 项目管理模块
UPDATE project p
INNER JOIN user_id_mapping m ON p.create_user = m.old_id
SET p.create_user = m.new_id
WHERE p.create_user IS NOT NULL;

UPDATE organization o
INNER JOIN user_id_mapping m ON o.create_user = m.old_id
SET o.create_user = m.new_id
WHERE o.create_user IS NOT NULL;

UPDATE workspace w
INNER JOIN user_id_mapping m ON w.create_user = m.old_id
SET w.create_user = m.new_id
WHERE w.create_user IS NOT NULL;

-- 8. 系统设置模块
UPDATE schedule s
INNER JOIN user_id_mapping m ON s.user_id = m.old_id
SET s.user_id = m.new_id
WHERE s.user_id IS NOT NULL;

UPDATE message_task mt
INNER JOIN user_id_mapping m ON mt.user_id = m.old_id
SET mt.user_id = m.new_id
WHERE mt.user_id IS NOT NULL;

UPDATE share_info si
INNER JOIN user_id_mapping m ON si.create_user_id = m.old_id
SET si.create_user_id = m.new_id
WHERE si.create_user_id IS NOT NULL;

-- 9. 其他通用字段
UPDATE api_module am
INNER JOIN user_id_mapping m ON am.create_user = m.old_id
SET am.create_user = m.new_id
WHERE am.create_user IS NOT NULL;

UPDATE api_scenario_module asm
INNER JOIN user_id_mapping m ON asm.create_user = m.old_id
SET asm.create_user = m.new_id
WHERE asm.create_user IS NOT NULL;

UPDATE test_case_node tcn
INNER JOIN user_id_mapping m ON tcn.create_user = m.old_id
SET tcn.create_user = m.new_id
WHERE tcn.create_user IS NOT NULL;

UPDATE file_metadata fm
INNER JOIN user_id_mapping m ON fm.create_user = m.old_id
SET fm.create_user = m.new_id
WHERE fm.create_user IS NOT NULL;

UPDATE file_metadata fm
INNER JOIN user_id_mapping m ON fm.update_user = m.old_id
SET fm.update_user = m.new_id
WHERE fm.update_user IS NOT NULL;

UPDATE operating_log ol
INNER JOIN user_id_mapping m ON ol.create_user = m.old_id
SET ol.create_user = m.new_id
WHERE ol.create_user IS NOT NULL;

-- ========================================
-- 最后更新user表主键（最关键的一步）
-- ========================================
UPDATE user u
INNER JOIN user_id_mapping m ON u.id = m.old_id
SET u.id = m.new_id;

-- 同时更新user表的create_user字段（如果有自引用）
UPDATE user u
INNER JOIN user_id_mapping m ON u.create_user = m.old_id
SET u.create_user = m.new_id
WHERE u.create_user IS NOT NULL;

-- ========================================
-- 验证数据完整性
-- ========================================

-- 检查是否所有记录都已更新
SELECT 
    '映射表记录数' as check_item,
    COUNT(*) as count
FROM user_id_mapping
UNION ALL
SELECT 
    'user表记录数',
    COUNT(*)
FROM user
UNION ALL
SELECT 
    'user_group更新后记录数',
    COUNT(*)
FROM user_group ug
INNER JOIN user u ON ug.user_id = u.id;

-- 检查是否有遗漏的旧ID（应该返回0）
SELECT 
    'user_group中的孤立记录' as check_item,
    COUNT(*) as orphan_count
FROM user_group ug
LEFT JOIN user u ON ug.user_id = u.id
WHERE u.id IS NULL;

-- 提交事务（确认无误后执行）
COMMIT;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 清理工作（可选，建议保留一段时间）
-- ========================================
-- 保留映射表至少1个月，便于问题追溯
-- DROP TABLE IF EXISTS user_id_mapping;
```

### 3.3 回滚脚本

```sql
-- ========================================
-- 紧急回滚脚本（出现问题时使用）
-- ========================================

-- 方案1：从备份恢复（最安全）
-- 在宿主机执行：
-- docker exec -i mysql mysql -uroot -p'Password123@mysql' metersphere_dev < backup_before_user_id_change_YYYYMMDD_HHMMSS.sql

-- 方案2：使用映射表反向回滚（如果映射表还在）
USE metersphere_dev;

SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- 反向更新所有表（将new_id改回old_id）
UPDATE user u
INNER JOIN user_id_mapping m ON u.id = m.new_id
SET u.id = m.old_id;

UPDATE user_group ug
INNER JOIN user_id_mapping m ON ug.user_id = m.new_id
SET ug.user_id = m.old_id;

-- ... 其他表同理 ...

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
```

## 四、执行时间评估（300用户规模）

| 阶段 | 预估时间 | 说明 |
|------|---------|------|
| 数据库备份 | 10-20分钟 | 取决于数据库大小 |
| 生成ID映射 | 1分钟 | 300条记录 |
| 人工审核映射 | 30-60分钟 | 确认新ID无冲突 |
| 更新关联表 | 30-60分钟 | 52张表，预估10万+记录 |
| 更新user表主键 | 2分钟 | 300条记录 |
| 数据验证 | 20-30分钟 | 抽查关键功能 |
| **总计** | **2-3小时** | 建议预留4小时窗口 |

## 五、风险控制措施

### 5.1 执行前准备（提前1周）

1. **测试环境完整演练**
   ```bash
   # 1. 复制生产数据到测试环境
   docker exec mysql mysqldump -uroot -p'Password123@mysql' metersphere_dev > prod_data.sql
   # 导入测试环境
   docker exec -i mysql_test mysql -uroot -ptest123 metersphere_test < prod_data.sql
   
   # 2. 在测试环境执行完整流程
   # 3. 验证所有功能模块
   # 4. 记录实际执行时间
   ```

2. **制定详细的执行计划**
   - 确定维护窗口：建议周六凌晨2:00-6:00
   - 通知所有用户：提前3天发送邮件通知
   - 准备应急联系人：DBA、开发负责人、运维负责人

3. **准备监控脚本**
   ```sql
   -- 实时监控更新进度
   SELECT 
       TABLE_NAME,
       TABLE_ROWS
   FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = 'metersphere_dev'
   AND TABLE_NAME IN ('user', 'user_group', 'test_case', 'api_definition')
   ORDER BY TABLE_NAME;
   ```

### 5.2 执行中监控

1. **分批提交**：每更新10张表提交一次事务
2. **实时验证**：每批次执行后立即验证数据
3. **记录日志**：保存所有SQL执行日志

### 5.3 执行后验证

```sql
-- 完整性验证脚本
-- 1. 检查user表记录数
SELECT COUNT(*) as user_count FROM user;

-- 2. 检查权限关联
SELECT 
    u.id, u.name, u.email,
    COUNT(ug.id) as group_count
FROM user u
LEFT JOIN user_group ug ON u.id = ug.user_id
GROUP BY u.id, u.name, u.email
HAVING group_count = 0;  -- 应该返回0条，否则说明有用户丢失权限

-- 3. 检查测试用例关联
SELECT 
    COUNT(*) as case_with_invalid_maintainer
FROM test_case tc
LEFT JOIN user u ON tc.maintainer = u.id
WHERE tc.maintainer IS NOT NULL AND u.id IS NULL;  -- 应该返回0

-- 4. 检查缺陷数据关联
SELECT 
    COUNT(*) as issues_count
FROM issues;  -- 确认缺陷数据总数未变化
```

## 六、功能验证清单

执行完成后，必须验证以下功能：

- [ ] 用户登录（测试3-5个不同用户）
- [ ] 权限验证（系统管理员、项目管理员、普通用户）
- [ ] 测试用例列表（查看维护人显示）
- [ ] 缺陷管理（查看创建人、处理人）
- [ ] 测试计划执行（查看执行人）
- [ ] 用例评审（查看评审人）
- [ ] 接口测试（查看创建人）
- [ ] 性能测试（查看创建人）
- [ ] 个人工作台（查看个人数据）
- [ ] 操作日志（查看历史记录）

## 七、常见问题处理

### Q1: 执行过程中断怎么办？
**A**: 立即执行回滚脚本，从备份恢复数据库，分析中断原因后重新执行。

### Q2: 发现部分用户无法登录？
**A**: 检查user_id_mapping表，确认该用户ID是否正确映射。可能是Session缓存问题，清除Redis缓存。

### Q3: 权限丢失怎么办？
**A**: 检查user_group表，执行验证SQL确认关联关系。如果确实丢失，从备份中恢复user_group表数据并重新执行映射。

### Q4: 历史数据显示异常？
**A**: 检查operating_log等日志表，确认create_user字段是否正确更新。

## 八、后续优化建议

1. **建立ID生成规范**：统一使用UUID或规范的前缀格式
2. **添加数据库约束**：在关键表添加外键约束，防止数据不一致
3. **定期数据审计**：每月检查一次用户关联数据完整性
4. **文档化**：将此次操作记录归档，作为后续参考

---

**最终建议**：
- ✅ 采用方案A（直接修改ID）
- ✅ 必须在测试环境完整验证
- ✅ 选择业务低峰期执行
- ✅ 准备完整的回滚预案
- ✅ 保留映射表至少1个月
