package io.metersphere.workflow.util;

public class BpmnXmlConverter {

    /**
     * Flowable BPMN 扩展命名空间声明。
     *
     * 说明：
     * - 前端建模器为了兼容性通常会输出 camunda:* 扩展；
     * - Flowable 引擎运行时更推荐使用 flowable:* 扩展（例如 assignee / candidateUsers）。
     * - 因此我们采用“编辑态保留 camunda，部署态转换为 flowable”的策略。
     */
    private static final String FLOWABLE_NS_DECL = "xmlns:flowable=\"http://flowable.org/bpmn\"";

    /**
     * 这些 sequenceFlow 在你的“8状态缺陷流转”模型里，来源节点是“只有一个出口的排他网关”。
     *
     * Flowable 校验规则：
     * - ExclusiveGateway（排他网关）如果只有 1 条 outgoing sequenceFlow，则该 outgoing 上不允许配置条件。
     * - 你目前的建模方式是为了“统一 toStatus 条件写法”，但在单出口场景会触发校验失败。
     *
     * 处理策略：
     * - 不改动编辑器输出（仍保留条件，保证前端能加载）；
     * - 部署前自动移除这几条线上的 conditionExpression（单出口无选择，条件也没有意义）。
     */
    private static final String[] SINGLE_OUTGOING_CONDITION_FLOWS = new String[] {
            "flow_new_to_accepted",       // gw_after_new -> task_accept
            "flow_on_hold_to_resolved",   // gw_after_on_hold -> task_resolved
            "flow_reopened_to_accept"     // gw_after_reopened -> task_accept
    };

    private BpmnXmlConverter() {
    }

    public static String toFlowableForDeployment(String xml) {
        if (xml == null) {
            return null;
        }

        String out = xml;

        // 1) camunda:* -> flowable:*
        // 说明：这是“部署态转换”，不影响你保存到数据库用于编辑的原始 XML。
        out = out.replace("camunda:assignee", "flowable:assignee");
        out = out.replace("camunda:candidateUsers", "flowable:candidateUsers");
        out = out.replace("camunda:candidateGroups", "flowable:candidateGroups");

        // 2) 补齐 xmlns:flowable（若缺失）
        // 说明：这里用 replaceFirst 做轻量级处理，避免引入 XML DOM 解析依赖。
        if (!out.contains("xmlns:flowable")) {
            out = out.replaceFirst("<bpmn:definitions\\s+", "<bpmn:definitions " + FLOWABLE_NS_DECL + " ");
        }

        // 3) 处理“单出口排他网关 + 条件表达式”导致的 Flowable 校验失败
        // 说明：仅针对已知的 3 条 sequenceFlow 做定向移除，避免误伤其他真正需要条件的分支。
        for (String flowId : SINGLE_OUTGOING_CONDITION_FLOWS) {
            out = stripConditionExpression(out, flowId);
        }

        return out;
    }

    /**
     * 移除指定 sequenceFlow 内的 <bpmn:conditionExpression ...>...</bpmn:conditionExpression> 节点。
     *
     * 说明：
     * - 使用正则的原因：不引入额外 XML 解析库，保持改动面小；
     * - 使用 [\s\S] 覆盖换行，非贪婪匹配避免跨越到其他节点。
     */
    private static String stripConditionExpression(String xml, String sequenceFlowId) {
        if (xml == null) {
            return null;
        }
        if (sequenceFlowId == null || sequenceFlowId.isBlank()) {
            return xml;
        }

        // 兼容两种写法：
        // 1) <bpmn:sequenceFlow ...> <bpmn:conditionExpression ...>...</bpmn:conditionExpression> </bpmn:sequenceFlow>
        // 2) conditionExpression 前后存在空白/换行
        String regex = "(<bpmn:sequenceFlow[^>]*id=\\\"" + sequenceFlowId + "\\\"[^>]*>)([\\s\\S]*?)<bpmn:conditionExpression[\\s\\S]*?</bpmn:conditionExpression>([\\s\\S]*?</bpmn:sequenceFlow>)";
        return xml.replaceAll(regex, "$1$2$3");
    }
}
