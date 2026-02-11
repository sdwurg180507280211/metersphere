USE metersphere_dev;

-- ========================================
-- 第一步：创建映射表
-- ========================================
DROP TABLE IF EXISTS user_id_mapping;

CREATE TABLE user_id_mapping (
old_id VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原用户ID',
new_id VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '新用户ID',
user_name VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '用户名（便于核对）',
user_email VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '用户邮箱（便于核对）',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (old_id),
UNIQUE KEY uk_new_id (new_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户ID映射表';

-- ========================================
-- 第二步：插入数据（从user表查询并填充用户名和邮箱）
-- ========================================
INSERT INTO user_id_mapping (old_id, new_id, user_name, user_email) VALUES
('anqi', 'rtdl_anqi', '安奇', NULL),
('bitaocun', 'trxl_biyaocun', '毕桃存', NULL),
('caoxueman', 'py_caoxueman', '曹雪曼', NULL),
('chaiqinglin', 'zkr_chaiqinglin', '柴青林', NULL),
('cheguanqing', 'ry_cheguanqing', '车冠青', NULL),
('chenda', 'zkr_chenda', '陈达', NULL),
('chenfangyuan', 'zkr_chenfangyuan', '陈方圆', NULL),
('chenguangxu', 'zkr_chenguangxu', '陈光绪', NULL),
('chenjunnan', 'zkr_chenjunnan', '陈俊男', NULL),
('chenlin', 'zkr_chenlin', '陈琳', NULL),
('chenmingyuan', 'zkr_chenmingyuan', '陈明源', NULL),
('chenqi', 'py_chenqi', '陈琦', NULL),
('chenxinyu', 'zkr_chenxinyu', '陈欣宇', NULL),
('chengru', 'sztb_chengru', '程茹', NULL),
('chengshuo', 'zkr_chengshuo', '程硕', NULL),
('cuichangchao', 'zkr_cuichangchao', '崔常超', NULL),
('cuiyonghu', 'zkr_cuiyonghu', '崔永虎', NULL),
('dairui', 'zkr_dairui', '戴睿', NULL),
('dixiaopeng', 'xzrj_dixiaopeng', '狄小朋', NULL),
('douchangping', 'zkr_douchangping', '窦常平', NULL),
('duxiaowei', 'zdxx_duxiaowei', '杜晓伟', NULL),
('fangyucheng', 'cl_wangyucheng', '方玉成', NULL),
('fengfuzhuang', 'cl_fengfuzhuang', '冯福壮', NULL),
('fengwenbo', 'zkr_fengwenbo', '冯文博', NULL),
('fengxiaowei', 'trxl_fengxiaowei', '冯晓伟', NULL),
('fengxueyan', 'py_fengxueyan', '冯雪艳', NULL),
('gaodejun', 'zkr_gaodejun', '高德军', NULL),
('gaojingcheng', 'trxl_gaojingcheng', '高景成', NULL),
('gaojingkui', 'xzrj_gaojingkui', '高靖奎', NULL),
('gaoxiang', 'zkr_gaoxiang', '高祥', NULL),
('guyinan', 'zkr_guyinan', '辜逸南', NULL),
('hansijia', 'dr_hansijia', '韩思嘉', NULL),
('hanweibin', 'ry_hanweibin', '韩伟斌', NULL),
('hanxiaoyan', 'xz_hanxiaoyan', '韩晓燕', NULL),
('haotingting', 'zkr_haotingting', '郝婷婷', NULL),
('houchenlin', 'sp_houchenlin', '侯辰林', NULL),
('huheran', 'zdjx_huheran', '胡贺然', NULL),
('hujinbao', 'zkr_hujinbao', '胡金保', NULL),
('huangliming', 'py_huangliming', '黄鹂鸣', NULL),
('jihaojie', 'zkr_jihaojie', '姬豪杰', NULL),
('jiajuanjuan', 'fsl_jiajuanjuan', '贾娟娟', NULL),
('jialiangbao', 'ry_jialiangbao', '贾良宝', NULL),
('jiaxiaomeng', 'zkr_jiaxiaomeng', '贾晓萌', NULL),
('jiangchengpeng', 'ry_jiangchengpeng', '姜程鹏', NULL),
('jianghaodong', 'zkr_jianghaodong', '姜浩东', NULL),
('jiangyanxu', 'py_jiangyanxu', '蒋艳旭', NULL),
('jiangyunlong', 'zkr_xiangyunlong', '降云龙', NULL),
('zkr_jiaoqingzhao', 'zkr_jiaoqingzhao001', '焦庆钊', NULL),
('jinjinrong', 'zkr_jinjinrong', '靳金融', NULL),
('kongyinyin', 'py_kongyinyin', '孔银银', NULL),
('lichenlong', 'shxz_lichenlong', '李辰龙', NULL),
('lierfei', 'sp_lierfei', '李二飞', NULL),
('zkr_lifang', 'zkr_lifang', '李芳', NULL),
('ligaoyuan', 'zkr_ligaoyuan001', '李高元', NULL),
('ligonglin', 'py_ligonglin', '李功林', NULL),
('liguoyin', 'zkr_liguoyin', '李国银', NULL),
('lihaotian', 'zdjx_lihaotian', '李昊天', NULL),
('liheng', 'zkr_liheng', '李恒', NULL),
('lijiwen', 'xz_lijiwen', '李记文', NULL),
('likun', 'zkr_likun', '李坤', NULL),
('limengrui', 'zkr_limengrui', '李梦蕊', NULL),
('lining', 'lining001', '李宁', NULL),
('lipanpan', 'zkr_lipanpan', '李盼盼', NULL),
('liqingjuan', 'zkr_liqingjuan', '李青娟', NULL),
('liqingao', 'sp_liqingao', '李庆澳', NULL),
('liruixing', 'zkr_liruixing', '李瑞星', NULL),
('litong', 'szsm_litong', '李彤', NULL),
('lixiaoling', 'ry_lixiaoling', '李晓玲', NULL),
('lixintong', 'zkr_lixintong', '李鑫童', NULL),
('liyasong', 'py_liyasong', '李亚松', NULL),
('liyan', 'zkr_liyan001', '李延', NULL),
('liyeru', 'xzrj_liyeru', '李叶茹', NULL),
('liyong', 'zkr_liyong', '李勇', NULL),
('liyusheng', 'py_liyusheng', '李雨生', NULL),
('lianglei', 'xzrj_lianglei', '梁磊', NULL),
('linlin', 'zkr_linlin', '林琳', NULL),
('liuchengchuan', 'zkr_liuchengchuan', '刘程川', NULL),
('liucong', 'ry_liucong', '刘从', NULL),
('liuhan', 'tb_liuhan', '刘晗', NULL),
('liujinsheng', 'zkr_liujinsheng', '刘进生', NULL),
('liulin', 'zkr_liulin', '刘林', NULL),
('zkf_liuningbo', 'zkr_liuningbo', '刘宁波', NULL),
('liuqian', 'szsm_liuqian', '刘倩', NULL),
('liutingting', 'cl_liutingting', '刘婷婷', NULL),
('liuweina', 'zkr_liuweina001', '刘伟娜', NULL),
('liuxiaosong', 'py_liuxiaosong', '刘筱松', NULL),
('liuxinshuai', 'zkr_liuxinshuai', '刘新帅', NULL),
('liuxuran', 'zkr_liuxuran', '刘旭冉', NULL),
('liuyao', 'ry_liuyao', '刘尧', NULL),
('liuyongjian', 'sp_liuyongjian', '刘永健', NULL),
('liuyuan', 'zkr_liuyuan', '刘源', NULL),
('liuyue', 'trxl_liuyue', '刘越', NULL),
('liuhao', 'dr_liuhao', '柳浩', NULL),
('ludaojuan', 'zkr_ludaojuan', '卢道娟', NULL),
('luozhifu', 'shxz_luozhifu', '罗志富', NULL),
('madongchen', 'zkr_madongchen001', '马东晨', NULL),
('maxiaodong', 'py_maxiaodong', '马晓东', NULL),
('mazhuanling', 'py_mazhuanling', '马转玲', NULL),
('maoqingbo', 'zkr_maoqingbo', '毛青博', NULL),
('menghaiyang', 'zdjx_menghaiyang', '孟海洋', NULL),
('mengshuang', 'sp_mengshuang', '孟双', NULL),
('mengxiangjuan', 'szsm_mengxiangjuan', '孟祥娟', NULL),
('niutong', 'zdjx_niutong', '牛通', NULL),
('niuzihao', 'xzrj_niuzihao', '牛子豪', NULL),
('panlong', 'kjls_panlong', '潘龙', NULL),
('pangshaoxia', 'dr_pangshaoxia', '庞少霞', NULL),
('qizhiyang', 'zkr_qizhiyang', '祁志洋', NULL),
('qiaozhanhao', 'zkr_qiaozhanhao', '乔占豪', NULL),
('qinyong', 'zkr_qinyong', '秦勇', NULL),
('renliting', 'zkr_renliting', '任丽婷', NULL),
('renmeng', 'zkr_renmeng', '任萌', NULL),
('shaowanru', 'ry_shaowanru', '邵婉如', NULL),
('shenguohui', 'zkr_shenguohui', '沈国辉', NULL),
('shixiaodou', 'zkr_shixiaodou', '石晓豆', NULL),
('shixiangwei', 'ry_shixiangwei', '史祥伟', NULL),
('shulai', 'zkr_shulai', '舒来', NULL),
('songchenhui', 'py_songchenhui', '宋晨辉', NULL),
('songpengsen', 'py_songpengsen', '宋鹏森', NULL),
('songyingxi', 'songyingqian', '宋迎茜', NULL),
('suning', 'zkr_suning', '苏宁', NULL),
('suzhaochuan', 'xzrj_suzhaochuan', '苏钊川', NULL),
('suipengyi', 'zkr_suipengyi', '随鹏艺', NULL),
('tianchuang', 'zkr_tianchuang', '田闯', NULL),
('tianyuezhen', 'szsm_tianyuezhen', '田月振', NULL),
('wangchenqian', 'ry_wangchengqian', '王承乾', NULL),
('wangdapeng', 'fsl_wangdapeng', '王大朋', NULL),
('wangdi', 'py_wangdi', '王帝', NULL),
('developer', 'wanghaiming', '王海明', NULL),
('wanghaipeng', 'py_wanghaipeng', '王海鹏', NULL),
('wanghongwei', 'py_wanghongwei', '王宏伟', NULL),
('wanghonglei', 'py_wanghonglei001', '王泓磊', NULL),
('wangjianzhong', 'ry_wangjianzhong', '王建忠', NULL),
('wangjing', 'zkr_wangjing', '王京', NULL),
('wangjing01', 'xzrj_wangjing', '王晶', NULL),
('wangliya', 'zdjx_wangliya', '王利亚', NULL),
('wangquan', 'zkr_wangquan', '王权', NULL),
('wangrui', 'zkr_wangrui', '王瑞', NULL),
('wangshenyang', 'xzrj_wangshenyang', '王沈阳', NULL),
('wangsi', 'ry_wangsi', '王思', NULL),
('wangxikai', 'zkr_wangxikai', '王希凯', NULL),
('wangxiaoxiao', 'zkr_wangxiaoxiao', '王笑笑', NULL),
('wangxinyu', 'szsm_wangxinyu', '王新宇', NULL),
('wangxuetong', 'ry_wangxuetong', '王学桐', NULL),
('wangxuehong', 'ry_wangxuehong', '王雪红', NULL),
('wangya', 'sp_wangya', '王亚', NULL),
('wangyafei', 'py_wangyafei', '王亚飞', NULL),
('wangyuhang', 'zdjx_wangyuhang', '王宇航', NULL),
('weishuang', 'py_weishuang', '韦爽', NULL),
('weidalin', 'py_weidalin', '魏大林', NULL),
('weisiqi', 'zkr_weisiqi', '魏思琪', NULL),
('wuzhiqiang', 'zkr_wuzhiqiang', '乌志强', NULL),
('wuna', 'py_wuna', '吴娜', NULL),
('wuyue', 'zkr_wuyue', '吴越', NULL),
('xitianqi', 'zkr_xitianqi', '席天琪', NULL),
('xiechaoyang', 'sztb_xiechaoyang', '谢朝阳', NULL),
('xieruimin', 'ry_xieruimin', '谢瑞敏', NULL),
('xinliangliang', 'zkr_xinliangliang', '辛亮亮', NULL),
('xingzhe', 'ry_xingzhe', '邢哲', NULL),
('xiongshixia', 'zkr_xiongshixia', '熊世霞', NULL),
('xiongyingbo', 'szsm_xiongyingbo', '熊营博', NULL),
('xiongzhengsheng', 'ry_xiongzhengsheng', '熊正胜', NULL),
('xubaiyue', 'ry_xubaiyue', '徐柏悦', NULL),
('xudoudou', 'zkr_xudoudou', '徐豆豆', NULL),
('xuxin', 'zkr_xuxin', '徐新', NULL),
('xuyanan', 'zkr_xuyanan', '徐亚南', NULL),
('xuzesen', 'zkr_xuzesen', '徐泽森', NULL),
('xuwanxia', 'zdjx_xuwanxia001', '许万霞', NULL),
('xuyakuan', 'zkr_xuyakuan', '许亚宽', NULL),
('xuanshiyun', 'szsm_xuanshiyun', '宣世昀', NULL),
('xuechuanhui', 'py_xuechuanhui', '薛传惠', NULL),
('yanhaiqiang', 'zkr_yanhaiqiang', '闫海强', NULL),
('yanlixia', 'zkr_yanlixia', '闫立侠', NULL),
('yangbo', 'zkr_yangbo', '杨波', NULL),
('yanghang', 'zkr_yanghang', '杨航', NULL),
('yangle', 'zkr_yangle', '杨乐', NULL),
('yangliu', 'ry_yangliu', '杨柳', NULL),
('yangqian', 'ry_yangqian', '杨仟', NULL),
('yangsen', 'zkr_yangsen', '杨森', NULL),
('yangyaxue', 'py_yangyaxue', '杨雅雪', NULL),
('yangyue', 'zkr_yangyue', '杨越', NULL),
('yangzheng', 'zkr_yangzheng', '杨征', NULL),
('yifenglei', 'py_yifenglei', '伊逢磊', NULL),
('yucaihua', 'py_yucaihua', '于彩华', NULL),
('yufengqiu', 'xz_yufengqiu', '于凤秋', NULL),
('yujie', 'ry_yujie', '于杰', NULL),
('yulu', 'zkr_yulu', '于璐', NULL),
('yuyao', 'dr_yuyao', '于瑶', NULL),
('yuzhiyu', 'py_yuzhiyu', '于智宇', NULL),
('yuexintao', 'zkr_yuexintao', '岳鑫涛', NULL),
('zhangaolong', 'zkr_zhangaolong', '张澳龙', NULL),
('zkr_zhangdongyan', 'zhangdongyan', '张东艳', NULL),
('zhangfenglin', 'shxz_zhangfenglin', '张锋林', NULL),
('zhangguoqing', 'cl_zhangguoqing', '张国庆', NULL),
('zhanghailong', 'szsm_zhanghailong', '张海龙', NULL),
('zhanghuancheng', 'py_zhanghuancheng', '张焕呈', NULL),
('zhangkai', 'tb_zhangkai', '张凯', NULL),
('zhangqihang', 'xzrj_zhangqihang', '张绮航', NULL),
('zhangsai', 'zkr_zhangsai', '张赛', NULL),
('zhangshuai', 'sp_zhangshuai', '张帅', NULL),
('zhangsuiqiu', 'py_zhangsuiqiu', '张随秋', NULL),
('zhangweiyu', 'zkr_zhangweiyu', '张伟昱', NULL),
('zhangxiaomin', 'py_zhangxiaomin', '张晓敏', NULL),
('zhangyongshuai', 'ry_zhangyongshuai', '张永帅', NULL),
('zhangyong', 'rtdl_zhangyong', '张勇', NULL),
('zhangchangqing', 'zkr_zhangchangqing', '张长庆', NULL),
('zhangzhaomei', 'cl_zhangzhaomei', '张兆梅', NULL),
('zhangziwei', 'sp_zhangziwei', '张子微', NULL),
('zhaoganglin', 'py_zhaoganglin', '赵刚林', NULL),
('zhaojieping', 'ry_zhaojieping', '赵杰平', NULL),
('zhaoliguo', 'xzrj_zhaoliguo', '赵立国', NULL),
('zhaorong', 'zkr_zhaorong', '赵蓉', NULL),
('zhaoxingyue', 'zkr_zhaoxingyue', '赵兴月', NULL),
('zhaoxuezhe', 'xzrj_zhaoxuezhe', '赵学哲', NULL),
('zhaoyue', 'zkr_zhaoyue', '赵越', NULL),
('zhenhaochuang', 'py_zhenhaochuang', '甄豪闯', NULL),
('zhengjie', 'zkr_zhengjie', '郑洁', NULL),
('zhengwenjing', 'xzrj_zhengwenjing', '郑文静', NULL),
('zhouhui', 'py_zhouhui', '周晖', NULL),
('zhuhonghui', 'xzrj_zhuhonghui', '朱宏慧', NULL),
('zhutianfu', 'szsm_zhutianfu', '朱天福', NULL),
('zhuzhanlong', 'zdjx_zhuzhanlong', '朱占龙', NULL),
('zhuhongwei', 'zkr_zhuhongwei', '祝宏伟', NULL),
('dongchaoyue', 'ry_dongchaoyue', '蒂超越', NULL),
('lijiangfei', 'lijianfei001', '李建飞', NULL),
('liuyujie', 'liuyujie102', '刘雨洁', NULL),
('xuheyan', 'zkr_xuheyan', '徐鹤严', NULL);


-- 插入带双引号的映射记录（用于自定义字段）
INSERT INTO user_id_mapping (old_id, new_id, user_name, user_email)
SELECT
CONCAT('"', old_id, '"') as old_id_quoted,
CONCAT('"', new_id, '"') as new_id_quoted,
user_name,
user_email
FROM user_id_mapping
WHERE old_id NOT LIKE '"%"';








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
-- 10. 自定义字段中的用户ID（重要！）
-- ========================================

-- 10.1 更新缺陷自定义字段中的用户ID
-- 处理人等成员类型字段存储在custom_field_issues表的value字段中
UPDATE custom_field_issues cfi
INNER JOIN user_id_mapping m ON cfi.value = m.old_id
SET cfi.value = m.new_id
WHERE cfi.value IS NOT NULL
AND cfi.value != ''
-- 只更新成员类型的字段
AND EXISTS (
SELECT 1 FROM custom_field cf
WHERE cf.id = cfi.field_id
AND cf.type = 'MEMBER'
);

-- 10.2 更新测试用例自定义字段中的用户ID
UPDATE custom_field_test_case cftc
INNER JOIN user_id_mapping m ON cftc.value = m.old_id
SET cftc.value = m.new_id
WHERE cftc.value IS NOT NULL
AND cftc.value != ''
AND EXISTS (
SELECT 1 FROM custom_field cf
WHERE cf.id = cftc.field_id
AND cf.type = 'MEMBER'
);

-- 10.3 更新接口自定义字段中的用户ID
UPDATE custom_field_api cfa
INNER JOIN user_id_mapping m ON cfa.value = m.old_id
SET cfa.value = m.new_id
WHERE cfa.value IS NOT NULL
AND cfa.value != ''
AND EXISTS (
SELECT 1 FROM custom_field cf
WHERE cf.id = cfa.field_id
AND cf.type = 'MEMBER'
);

-- 10.4 处理多选成员字段（value存储的是JSON数组）
-- 注意：如果成员字段支持多选，value可能是 ["user1","user2"] 格式
-- 这种情况需要特殊处理，建议使用存储过程或应用层处理

-- 检查是否有多选成员字段
SELECT
cf.id,
cf.name,
cf.type,
cf.scene,
COUNT(*) as usage_count
FROM custom_field cf
WHERE cf.type = 'MEMBER'
GROUP BY cf.id, cf.name, cf.type, cf.scene;

-- 如果发现有多选成员字段，需要额外处理
-- 示例：处理custom_field_issues表中的JSON数组格式
-- UPDATE custom_field_issues cfi
-- INNER JOIN user_id_mapping m
-- SET cfi.value = JSON_REPLACE(cfi.value, '$[*]', m.new_id)
-- WHERE JSON_CONTAINS(cfi.value, JSON_QUOTE(m.old_id))
-- AND EXISTS (
--     SELECT 1 FROM custom_field cf
--     WHERE cf.id = cfi.field_id
--     AND cf.type = 'MEMBER'
-- );

-- ========================================
-- 11. 缺陷关注人和历史记录
-- ========================================

-- 11.1 更新缺陷关注人表
UPDATE issue_follow if_table
INNER JOIN user_id_mapping m ON if_table.follow_id = m.old_id
SET if_table.follow_id = m.new_id;

-- 11.2 更新缺陷变更记录表（操作人字段）
UPDATE issue_change_log icl
INNER JOIN user_id_mapping m ON icl.operator = m.old_id
SET icl.operator = m.new_id
WHERE icl.operator IS NOT NULL;

-- 11.3 更新测试用例关注人表
UPDATE test_case_follow tcf
INNER JOIN user_id_mapping m ON tcf.follow_id = m.old_id
SET tcf.follow_id = m.new_id;

-- 11.4 更新接口定义关注人表
UPDATE api_definition_follow adf
INNER JOIN user_id_mapping m ON adf.follow_id = m.old_id
SET adf.follow_id = m.new_id;

-- 11.5 更新接口场景关注人表（如果存在）
-- UPDATE api_scenario_follow asf
-- INNER JOIN user_id_mapping m ON asf.follow_id = m.old_id
-- SET asf.follow_id = m.new_id;

-- 11.6 更新接口测试用例关注人表
UPDATE api_test_case_follow atcf
INNER JOIN user_id_mapping m ON atcf.follow_id = m.old_id
SET atcf.follow_id = m.new_id;

-- 注意：operating_log表已在第9节更新，无需重复处理

-- ========================================
-- 12. 其他遗漏的表（补充）
-- ========================================

-- 12.1 更新API相关表
UPDATE api_definition_env ade
INNER JOIN user_id_mapping m ON ade.user_id = m.old_id
SET ade.user_id = m.new_id
WHERE ade.user_id IS NOT NULL;

UPDATE api_definition_exec_result ader
INNER JOIN user_id_mapping m ON ader.user_id = m.old_id
SET ader.user_id = m.new_id
WHERE ader.user_id IS NOT NULL;

UPDATE api_scenario_reference_id asri
INNER JOIN user_id_mapping m ON asri.create_user_id = m.old_id
SET asri.create_user_id = m.new_id
WHERE asri.create_user_id IS NOT NULL;

UPDATE api_test_environment ate
INNER JOIN user_id_mapping m ON ate.create_user = m.old_id
SET ate.create_user = m.new_id
WHERE ate.create_user IS NOT NULL;

UPDATE api_test_report atr
INNER JOIN user_id_mapping m ON atr.user_id = m.old_id
SET atr.user_id = m.new_id
WHERE atr.user_id IS NOT NULL;

UPDATE api_template at_table
INNER JOIN user_id_mapping m ON at_table.create_user = m.old_id
SET at_table.create_user = m.new_id
WHERE at_table.create_user IS NOT NULL;

UPDATE api_test at_test
INNER JOIN user_id_mapping m ON at_test.user_id = m.old_id
SET at_test.user_id = m.new_id
WHERE at_test.user_id IS NOT NULL;

-- 12.2 更新自定义字段和函数表
UPDATE custom_field cf
INNER JOIN user_id_mapping m ON cf.create_user = m.old_id
SET cf.create_user = m.new_id
WHERE cf.create_user IS NOT NULL;

UPDATE custom_function cf_func
INNER JOIN user_id_mapping m ON cf_func.create_user = m.old_id
SET cf_func.create_user = m.new_id
WHERE cf_func.create_user IS NOT NULL;

-- 12.3 更新企业报告相关表
UPDATE enterprise_test_report etr
INNER JOIN user_id_mapping m ON etr.create_user = m.old_id
SET etr.create_user = m.new_id
WHERE etr.create_user IS NOT NULL;

UPDATE enterprise_test_report etr
INNER JOIN user_id_mapping m ON etr.update_user = m.old_id
SET etr.update_user = m.new_id
WHERE etr.update_user IS NOT NULL;

UPDATE enterprise_test_report_send_record etrsr
INNER JOIN user_id_mapping m ON etrsr.create_user = m.old_id
SET etrsr.create_user = m.new_id
WHERE etrsr.create_user IS NOT NULL;

-- 12.4 更新环境组表
UPDATE environment_group eg
INNER JOIN user_id_mapping m ON eg.create_user = m.old_id
SET eg.create_user = m.new_id
WHERE eg.create_user IS NOT NULL;

-- 12.5 更新错误报告库表
UPDATE error_report_library erl
INNER JOIN user_id_mapping m ON erl.create_user = m.old_id
SET erl.create_user = m.new_id
WHERE erl.create_user IS NOT NULL;

UPDATE error_report_library erl
INNER JOIN user_id_mapping m ON erl.update_user = m.old_id
SET erl.update_user = m.new_id
WHERE erl.update_user IS NOT NULL;

-- 12.6 更新文件附件表
UPDATE file_attachment_metadata fam
INNER JOIN user_id_mapping m ON fam.creator = m.old_id
SET fam.creator = m.new_id
WHERE fam.creator IS NOT NULL;

-- 12.7 更新文件模块表
UPDATE file_module fm_module
INNER JOIN user_id_mapping m ON fm_module.create_user = m.old_id
SET fm_module.create_user = m.new_id
WHERE fm_module.create_user IS NOT NULL;

-- 注意：file_module表的repository_user_name字段是仓库用户名，不是系统用户ID，无需更新

-- 12.8 更新用户组表
UPDATE `group` g
INNER JOIN user_id_mapping m ON g.creator = m.old_id
SET g.creator = m.new_id
WHERE g.creator IS NOT NULL;

-- 12.9 更新缺陷模板表
UPDATE issue_template it_table
INNER JOIN user_id_mapping m ON it_table.create_user = m.old_id
SET it_table.create_user = m.new_id
WHERE it_table.create_user IS NOT NULL;

-- 12.10 更新缺陷创建人字段
UPDATE issues i
INNER JOIN user_id_mapping m ON i.creator = m.old_id
SET i.creator = m.new_id
WHERE i.creator IS NOT NULL;

-- 12.11 更新JAR配置表
UPDATE jar_config jc
INNER JOIN user_id_mapping m ON jc.creator = m.old_id
SET jc.creator = m.new_id
WHERE jc.creator IS NOT NULL;

-- 12.12 更新性能测试关注表
UPDATE load_test_follow ltf
INNER JOIN user_id_mapping m ON ltf.follow_id = m.old_id
SET ltf.follow_id = m.new_id;

-- 12.13 更新Mock配置表
UPDATE mock_config mc
INNER JOIN user_id_mapping m ON mc.create_user_id = m.old_id
SET mc.create_user_id = m.new_id
WHERE mc.create_user_id IS NOT NULL;

UPDATE mock_expect_config mec
INNER JOIN user_id_mapping m ON mec.create_user_id = m.old_id
SET mec.create_user_id = m.new_id
WHERE mec.create_user_id IS NOT NULL;

-- 12.14 更新新手统计表
UPDATE novice_statistics ns
INNER JOIN user_id_mapping m ON ns.user_id = m.old_id
SET ns.user_id = m.new_id;

-- 12.15 更新操作日志的操作人字段
UPDATE operating_log ol
INNER JOIN user_id_mapping m ON ol.oper_user = m.old_id
SET ol.oper_user = m.new_id
WHERE ol.oper_user IS NOT NULL;

-- 12.16 更新插件表
UPDATE plugin p
INNER JOIN user_id_mapping m ON p.create_user_id = m.old_id
SET p.create_user_id = m.new_id
WHERE p.create_user_id IS NOT NULL;

-- 12.17 更新项目版本表
UPDATE project_version pv
INNER JOIN user_id_mapping m ON pv.create_user = m.old_id
SET pv.create_user = m.new_id
WHERE pv.create_user IS NOT NULL;

-- 12.18 更新关系边表
UPDATE relationship_edge re
INNER JOIN user_id_mapping m ON re.creator = m.old_id
SET re.creator = m.new_id
WHERE re.creator IS NOT NULL;

-- 12.19 更新报告统计表
UPDATE report_statistics rs
INNER JOIN user_id_mapping m ON rs.create_user = m.old_id
SET rs.create_user = m.new_id
WHERE rs.create_user IS NOT NULL;

UPDATE report_statistics rs
INNER JOIN user_id_mapping m ON rs.update_user = m.old_id
SET rs.update_user = m.new_id
WHERE rs.update_user IS NOT NULL;

-- 12.20 更新测试用例报告相关表
UPDATE test_case_report tcr
INNER JOIN user_id_mapping m ON tcr.create_user = m.old_id
SET tcr.create_user = m.new_id
WHERE tcr.create_user IS NOT NULL;

UPDATE test_case_report_template tcrt
INNER JOIN user_id_mapping m ON tcrt.create_user = m.old_id
SET tcrt.create_user = m.new_id
WHERE tcrt.create_user IS NOT NULL;

-- 12.21 更新测试用例评审关注表
UPDATE test_case_review_follow tcrf
INNER JOIN user_id_mapping m ON tcrf.follow_id = m.old_id
SET tcrf.follow_id = m.new_id;

-- 12.22 更新测试用例评审的creator字段（与create_user不同）
UPDATE test_case_review tcr_review
INNER JOIN user_id_mapping m ON tcr_review.creator = m.old_id
SET tcr_review.creator = m.new_id
WHERE tcr_review.creator IS NOT NULL;

-- 12.23 更新测试用例模板表
UPDATE test_case_template tct
INNER JOIN user_id_mapping m ON tct.create_user = m.old_id
SET tct.create_user = m.new_id
WHERE tct.create_user IS NOT NULL;

-- 12.23.1 更新测试用例评审节点表
UPDATE test_case_review_node tcrn
INNER JOIN user_id_mapping m ON tcrn.create_user = m.old_id
SET tcrn.create_user = m.new_id
WHERE tcrn.create_user IS NOT NULL;

-- 12.23.2 更新测试计划节点表
UPDATE test_plan_node tpn
INNER JOIN user_id_mapping m ON tpn.create_user = m.old_id
SET tpn.create_user = m.new_id
WHERE tpn.create_user IS NOT NULL;

-- 12.24 更新测试计划相关表
UPDATE test_plan tp
INNER JOIN user_id_mapping m ON tp.creator = m.old_id
SET tp.creator = m.new_id
WHERE tp.creator IS NOT NULL;

UPDATE test_plan_api_case tpac
INNER JOIN user_id_mapping m ON tpac.create_user = m.old_id
SET tpac.create_user = m.new_id
WHERE tpac.create_user IS NOT NULL;

UPDATE test_plan_api_scenario tpas
INNER JOIN user_id_mapping m ON tpas.create_user = m.old_id
SET tpas.create_user = m.new_id
WHERE tpas.create_user IS NOT NULL;

UPDATE test_plan_execution_queue tpeq
INNER JOIN user_id_mapping m ON tpeq.execute_user = m.old_id
SET tpeq.execute_user = m.new_id
WHERE tpeq.execute_user IS NOT NULL;

UPDATE test_plan_follow tpf
INNER JOIN user_id_mapping m ON tpf.follow_id = m.old_id
SET tpf.follow_id = m.new_id;

UPDATE test_plan_load_case tplc
INNER JOIN user_id_mapping m ON tplc.create_user = m.old_id
SET tplc.create_user = m.new_id
WHERE tplc.create_user IS NOT NULL;

UPDATE test_plan_report tpr
INNER JOIN user_id_mapping m ON tpr.creator = m.old_id
SET tpr.creator = m.new_id
WHERE tpr.creator IS NOT NULL;

UPDATE test_plan_report tpr
INNER JOIN user_id_mapping m ON tpr.principal = m.old_id
SET tpr.principal = m.new_id
WHERE tpr.principal IS NOT NULL;

UPDATE test_plan_ui_scenario tpus
INNER JOIN user_id_mapping m ON tpus.create_user = m.old_id
SET tpus.create_user = m.new_id
WHERE tpus.create_user IS NOT NULL;

-- 12.25 更新UI测试相关表
UPDATE ui_element ue
INNER JOIN user_id_mapping m ON ue.create_user = m.old_id
SET ue.create_user = m.new_id
WHERE ue.create_user IS NOT NULL;

UPDATE ui_element ue
INNER JOIN user_id_mapping m ON ue.update_user = m.old_id
SET ue.update_user = m.new_id
WHERE ue.update_user IS NOT NULL;

UPDATE ui_element_module uem
INNER JOIN user_id_mapping m ON uem.create_user = m.old_id
SET uem.create_user = m.new_id
WHERE uem.create_user IS NOT NULL;

UPDATE ui_scenario_module usm
INNER JOIN user_id_mapping m ON usm.create_user = m.old_id
SET usm.create_user = m.new_id
WHERE usm.create_user IS NOT NULL;

UPDATE ui_scenario_reference usr
INNER JOIN user_id_mapping m ON usr.create_user_id = m.old_id
SET usr.create_user_id = m.new_id
WHERE usr.create_user_id IS NOT NULL;

-- 12.26 更新通知表
UPDATE notification n
INNER JOIN user_id_mapping m ON n.operator = m.old_id
SET n.operator = m.new_id
WHERE n.operator IS NOT NULL;
------------------------------------------------
UPDATE notification n
INNER JOIN user_id_mapping m ON n.receiver = m.old_id
SET n.receiver = m.new_id
WHERE n.receiver IS NOT NULL;

-- ========================================
-- 13. 汇总表中缺失的UPDATE语句（补充）
-- ========================================

-- 13.1 更新接口场景关注人表（取消注释，汇总表序号8）
UPDATE api_scenario_follow asf
INNER JOIN user_id_mapping m ON asf.follow_id = m.old_id
SET asf.follow_id = m.new_id;

-- 13.2 更新缺陷评论表（汇总表序号27）
UPDATE issue_comment ic
INNER JOIN user_id_mapping m ON ic.author = m.old_id
SET ic.author = m.new_id
WHERE ic.author IS NOT NULL;

-- 13.3 更新缺陷报告人字段（汇总表序号29，之前只更新了creator）
UPDATE issues i
INNER JOIN user_id_mapping m ON i.reporter = m.old_id
SET i.reporter = m.new_id
WHERE i.reporter IS NOT NULL;

-- 13.4 更新缺陷变更记录详情表中的用户ID（汇总表序号25、26）
-- 注意：old_value和new_value中存储的是用户ID，需要替换
UPDATE issue_change_log_detail icld
INNER JOIN user_id_mapping m ON icld.old_value = m.old_id
SET icld.old_value = m.new_id
WHERE icld.old_value IS NOT NULL;

UPDATE issue_change_log_detail icld
INNER JOIN user_id_mapping m ON icld.new_value = m.old_id
SET icld.new_value = m.new_id
WHERE icld.new_value IS NOT NULL;

-- 13.5 更新测试用例评论表（汇总表序号48）
UPDATE test_case_comment tcc
INNER JOIN user_id_mapping m ON tcc.author = m.old_id
SET tcc.author = m.new_id
WHERE tcc.author IS NOT NULL;

-- 13.6 更新测试计划负责人表（汇总表序号64）
UPDATE test_plan_principal tpp
INNER JOIN user_id_mapping m ON tpp.principal_id = m.old_id
SET tpp.principal_id = m.new_id
WHERE tpp.principal_id IS NOT NULL;

-- 13.7 更新UI场景报告表（汇总表序号76、77）
UPDATE ui_scenario_report usr_report
INNER JOIN user_id_mapping m ON usr_report.user_id = m.old_id
SET usr_report.user_id = m.new_id
WHERE usr_report.user_id IS NOT NULL;

UPDATE ui_scenario_report usr_report
INNER JOIN user_id_mapping m ON usr_report.create_user = m.old_id
SET usr_report.create_user = m.new_id
WHERE usr_report.create_user IS NOT NULL;

-- 13.8 更新备份表（汇总表序号23、81）
-- 注意：备份表可选更新，如不需要可跳过
UPDATE group_backup_20260126 gb
INNER JOIN user_id_mapping m ON gb.creator = m.old_id
SET gb.creator = m.new_id
WHERE gb.creator IS NOT NULL;

UPDATE user_group_backup_20260126 ugb
INNER JOIN user_id_mapping m ON ugb.user_id = m.old_id
SET ugb.user_id = m.new_id;
-----------------------------------------------
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

-- 检查自定义字段中的用户ID是否都已更新
SELECT
'custom_field_issues中未更新的成员字段' as check_item,
COUNT(*) as orphan_count
FROM custom_field_issues cfi
INNER JOIN custom_field cf ON cfi.field_id = cf.id
LEFT JOIN user u ON cfi.value = u.id
WHERE cf.type = 'MEMBER'
AND cfi.value IS NOT NULL
AND cfi.value != ''
AND u.id IS NULL;

SELECT
'custom_field_test_case中未更新的成员字段' as check_item,
COUNT(*) as orphan_count
FROM custom_field_test_case cftc
INNER JOIN custom_field cf ON cftc.field_id = cf.id
LEFT JOIN user u ON cftc.value = u.id
WHERE cf.type = 'MEMBER'
AND cftc.value IS NOT NULL
AND cftc.value != ''
AND u.id IS NULL;

-- 检查关注人表中的用户ID是否都已更新
SELECT
'issue_follow中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM issue_follow if_table
LEFT JOIN user u ON if_table.follow_id = u.id
WHERE u.id IS NULL;

SELECT
'test_case_follow中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM test_case_follow tcf
LEFT JOIN user u ON tcf.follow_id = u.id
WHERE u.id IS NULL;

SELECT
'test_case_review_follow中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM test_case_review_follow tcrf
LEFT JOIN user u ON tcrf.follow_id = u.id
WHERE u.id IS NULL;

SELECT
'test_plan_follow中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM test_plan_follow tpf
LEFT JOIN user u ON tpf.follow_id = u.id
WHERE u.id IS NULL;

SELECT
'load_test_follow中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM load_test_follow ltf
LEFT JOIN user u ON ltf.follow_id = u.id
WHERE u.id IS NULL;

-- 检查api_test_case_follow中的孤立记录
SELECT
'api_test_case_follow中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM api_test_case_follow atcf
LEFT JOIN user u ON atcf.follow_id = u.id
WHERE u.id IS NULL;

-- 检查test_case_review_node中的孤立记录
SELECT
'test_case_review_node中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM test_case_review_node tcrn
LEFT JOIN user u ON tcrn.create_user = u.id
WHERE tcrn.create_user IS NOT NULL AND u.id IS NULL;

-- 检查test_plan_node中的孤立记录
SELECT
'test_plan_node中的孤立记录' as check_item,
COUNT(*) as orphan_count
FROM test_plan_node tpn
LEFT JOIN user u ON tpn.create_user = u.id
WHERE tpn.create_user IS NOT NULL AND u.id IS NULL;

-- 提交事务（确认无误后执行）
COMMIT;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 清理工作（可选，建议保留一段时间）
-- ========================================
-- 保留映射表至少1个月，便于问题追溯
-- DROP TABLE IF EXISTS user_id_mapping;
